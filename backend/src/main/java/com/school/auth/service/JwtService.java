package com.school.auth.service;

import com.school.auth.model.Role;
import com.school.auth.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates JWT access tokens. The configured secret is hashed
 * with SHA-256 to always yield a valid HS256 key length, regardless of how
 * long the raw secret string is (dev defaults are short on purpose).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_RECOMMENDED_SECRET_LENGTH = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be set");
        }
        if (properties.secret().length() < MIN_RECOMMENDED_SECRET_LENGTH) {
            log.warn("JWT_SECRET is shorter than {} characters — fine for local dev, but anyone who guesses it "
                    + "can forge tokens. Set a long random value before any non-local deployment.",
                    MIN_RECOMMENDED_SECRET_LENGTH);
        }
        this.key = deriveKey(properties.secret());
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String generateToken(String subjectEmail, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subjectEmail)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Duration getExpiration() {
        return expiration;
    }

    /**
     * Short-lived signed token for non-login purposes (e.g. the OAuth
     * "state" parameter). It carries no "role" claim, so
     * JwtAuthenticationFilter never accepts it as a login.
     */
    public String generatePurposeToken(String subject, String purpose, java.util.Map<String, String> claims,
                                       Duration ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .claim("purpose", purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        claims.forEach(builder::claim);
        return builder.signWith(key).compact();
    }

    /** Claims of a valid, unexpired purpose token with the given purpose; empty otherwise. */
    public Optional<Claims> parsePurposeToken(String token, String purpose) {
        return parse(token).filter(c -> purpose.equals(c.get("purpose", String.class)) && c.get("role") == null);
    }

    /**
     * Returns the parsed claims if the token is well-formed, correctly
     * signed, and not expired; empty otherwise. Never throws for malformed
     * or expired tokens.
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
