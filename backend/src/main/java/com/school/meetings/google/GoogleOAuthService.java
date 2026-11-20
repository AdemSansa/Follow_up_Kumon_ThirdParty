package com.school.meetings.google;

import com.google.auth.oauth2.UserCredentials;
import com.school.auth.service.JwtService;
import com.school.common.exception.BadRequestException;
import com.school.meetings.MeetingProperties;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Connect Google" (OAuth 2.0 authorization-code flow, offline access) and
 * access tokens for connected accounts. Free Gmail accounts work.
 */
@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    /** Create/update/delete events (with Meet) + know which Google account was connected. */
    static final String SCOPES = "openid email https://www.googleapis.com/auth/calendar.events";
    static final String STATE_PURPOSE = "google-oauth";

    public enum Kind { CENTER, PERSONAL }

    private final MeetingProperties properties;
    private final GoogleAccountRepository accounts;
    private final TokenCipher cipher;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final RestClient http = RestClient.create();
    private final Map<UUID, UserCredentials> credentials = new ConcurrentHashMap<>();

    public GoogleOAuthService(MeetingProperties properties, GoogleAccountRepository accounts, TokenCipher cipher,
                              JwtService jwtService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.accounts = accounts;
        this.cipher = cipher;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return properties.google() != null && properties.google().isConfigured();
    }

    public String notConfiguredReason() {
        return "Google sign-in is not set up on the server (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET) "
                + "— see docs/features/google-meet.md";
    }

    /** The Google consent URL for {@code userId} connecting the center account or their own. */
    public String authorizationUrl(UUID userId, Kind kind) {
        if (!isConfigured()) {
            throw new BadRequestException(notConfiguredReason());
        }
        String state = jwtService.generatePurposeToken(userId.toString(), STATE_PURPOSE,
                Map.of("kind", kind.name()), Duration.ofMinutes(10));
        return UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("client_id", properties.google().clientId())
                .queryParam("redirect_uri", properties.google().redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("access_type", "offline")      // we need a refresh token for the background job
                .queryParam("prompt", "consent select_account") // always return a refresh token; let them pick the account
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    /**
     * Handles Google's redirect: verifies state, exchanges the code, stores the
     * (encrypted) refresh token. Returns the connected Google email.
     */
    @Transactional
    public String completeConnection(String code, String state) {
        Claims claims = jwtService.parsePurposeToken(state, STATE_PURPOSE)
                .orElseThrow(() -> new BadRequestException("The Google sign-in link expired — try connecting again"));
        UUID userId = UUID.fromString(claims.getSubject());
        Kind kind = Kind.valueOf(claims.get("kind", String.class));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.google().clientId());
        form.add("client_secret", properties.google().clientSecret());
        form.add("redirect_uri", properties.google().redirectUri());
        form.add("grant_type", "authorization_code");
        JsonNode tokens;
        try {
            tokens = http.post().uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new BadRequestException("Google refused the sign-in (" + e.getStatusCode().value() + ")");
        }
        String refreshToken = tokens.path("refresh_token").asText(null);
        if (refreshToken == null) {
            throw new BadRequestException("Google didn't return offline access. Remove the app at "
                    + "https://myaccount.google.com/permissions and connect again.");
        }
        if (!tokens.path("scope").asText("").contains("calendar.events")) {
            throw new BadRequestException("Calendar access was not granted — tick the calendar permission when "
                    + "connecting.");
        }
        String email = emailFromIdToken(tokens.path("id_token").asText(""));

        GoogleAccount account = kind == Kind.CENTER
                ? accounts.findFirstByCenterTrue().orElseGet(GoogleAccount::centerAccount)
                : accounts.findByUserId(userId).orElseGet(() -> GoogleAccount.personal(userId));
        account.connect(email, cipher.encrypt(refreshToken), tokens.path("scope").asText(null), userId);
        account = accounts.save(account);
        credentials.remove(account.getId());
        log.info("Google account {} connected as {} by user {}", email, kind, userId);
        return email;
    }

    @Transactional
    public void disconnect(GoogleAccount account) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("token", cipher.decrypt(account.getRefreshTokenEnc()));
            http.post().uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.info("Revoking Google token failed (removing it anyway): {}", e.getMessage());
        }
        credentials.remove(account.getId());
        accounts.delete(account);
    }

    /** A fresh access token for a connected account; records the error on the account if Google refuses. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalStateException.class)
    public String accessToken(UUID accountId) {
        GoogleAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("That Google account is no longer connected"));
        try {
            UserCredentials creds = credentials.computeIfAbsent(accountId, id -> UserCredentials.newBuilder()
                    .setClientId(properties.google().clientId())
                    .setClientSecret(properties.google().clientSecret())
                    .setRefreshToken(cipher.decrypt(account.getRefreshTokenEnc()))
                    .build());
            creds.refreshIfExpired();
            if (account.getLastError() != null) {
                account.setLastError(null);
            }
            return creds.getAccessToken().getTokenValue();
        } catch (IOException | RuntimeException e) {
            credentials.remove(accountId);
            String reason = "Google access for " + account.getGoogleEmail() + " stopped working ("
                    + e.getMessage() + ") — reconnect it in Settings";
            account.setLastError(reason);
            throw new IllegalStateException(reason, e);
        }
    }

    public Optional<GoogleAccount> center() {
        return accounts.findFirstByCenterTrue();
    }

    public Optional<GoogleAccount> personal(UUID userId) {
        return accounts.findByUserId(userId);
    }

    private String emailFromIdToken(String idToken) {
        // Received directly from Google's token endpoint over TLS, so the payload can be read without
        // re-verifying the signature (Google's documented guidance for this case).
        try {
            String[] parts = idToken.split("\\.");
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            String email = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8)).path("email").asText(null);
            if (email == null) {
                throw new IllegalStateException("no email claim");
            }
            return email;
        } catch (RuntimeException e) {
            throw new BadRequestException("Couldn't read which Google account was connected — try again");
        }
    }
}
