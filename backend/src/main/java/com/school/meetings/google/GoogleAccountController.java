package com.school.meetings.google;

import com.school.auth.model.User;
import com.school.auth.repository.UserRepository;
import com.school.common.exception.BadRequestException;
import com.school.meetings.MeetingProperties;
import com.school.meetings.google.GoogleOAuthService.Kind;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * Settings → "Google account": connect/disconnect the shared center account
 * (admins) or your own account (anyone). The OAuth callback is public — it's
 * a browser redirect from Google, authenticated by the signed state token.
 */
@RestController
@RequestMapping("/api/google")
public class GoogleAccountController {

    private final GoogleOAuthService oauth;
    private final UserRepository users;
    private final MeetingProperties properties;

    public GoogleAccountController(GoogleOAuthService oauth, UserRepository users, MeetingProperties properties) {
        this.oauth = oauth;
        this.users = users;
        this.properties = properties;
    }

    public record AccountView(String googleEmail, Instant connectedAt, String lastError) {
        static AccountView of(GoogleAccount a) {
            return new AccountView(a.getGoogleEmail(), a.getConnectedAt(), a.getLastError());
        }
    }

    public record AccountsResponse(boolean configured, String configMessage, AccountView center, AccountView mine) {
    }

    @GetMapping("/accounts")
    public AccountsResponse accounts(Authentication auth) {
        User me = currentUser(auth);
        return new AccountsResponse(
                oauth.isConfigured(),
                oauth.isConfigured() ? "" : oauth.notConfiguredReason(),
                oauth.center().map(AccountView::of).orElse(null),
                oauth.personal(me.getId()).map(AccountView::of).orElse(null));
    }

    /** Returns the Google consent URL; the frontend sends the browser there. CENTER requires ADMIN. */
    @PostMapping("/accounts/connect")
    public Map<String, String> connect(@RequestParam Kind kind, Authentication auth) {
        User me = currentUser(auth);
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (kind == Kind.CENTER && !admin) {
            throw new org.springframework.security.access.AccessDeniedException("Only an admin can connect the center account");
        }
        return Map.of("url", oauth.authorizationUrl(me.getId(), kind));
    }

    @DeleteMapping("/accounts/center")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disconnectCenter() {
        oauth.center().ifPresent(oauth::disconnect);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/accounts/mine")
    public ResponseEntity<Void> disconnectMine(Authentication auth) {
        oauth.personal(currentUser(auth).getId()).ifPresent(oauth::disconnect);
        return ResponseEntity.noContent().build();
    }

    /** Google redirects here after consent; we bounce the browser back to the app's Settings page. */
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        UriComponentsBuilder back = UriComponentsBuilder.fromUriString(properties.frontendUrl()).path("/settings");
        if (error != null || code == null || state == null) {
            back.queryParam("google", "error").queryParam("reason",
                    "access_denied".equals(error) ? "You cancelled the Google sign-in." : "Google sign-in failed.");
        } else {
            try {
                String email = oauth.completeConnection(code, state);
                back.queryParam("google", "connected").queryParam("email", email);
            } catch (BadRequestException e) {
                back.queryParam("google", "error").queryParam("reason", e.getMessage());
            }
        }
        URI location = back.encode().build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location.toString()).build();
    }

    private User currentUser(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new BadRequestException("Unknown user"));
    }
}
