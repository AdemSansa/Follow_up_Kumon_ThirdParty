package com.school.auth.service;

import com.school.auth.model.Role;
import com.school.auth.security.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatesTokenWithSubjectAndRoleClaim() {
        JwtService service = new JwtService(new JwtProperties("test-secret", 60));

        String token = service.generateToken("teacher@example.com", Role.STAFF);
        Optional<Claims> claims = service.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("teacher@example.com");
        assertThat(claims.get().get("role", String.class)).isEqualTo("STAFF");
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService issuer = new JwtService(new JwtProperties("secret-one", 60));
        JwtService verifier = new JwtService(new JwtProperties("secret-two", 60));

        String token = issuer.generateToken("admin@example.com", Role.ADMIN);

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService(new JwtProperties("test-secret", -5));

        String token = service.generateToken("admin@example.com", Role.ADMIN);

        assertThat(service.parse(token)).isEmpty();
    }

    @Test
    void rejectsGarbageToken() {
        JwtService service = new JwtService(new JwtProperties("test-secret", 60));

        assertThat(service.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void toleratesVeryShortConfiguredSecret() {
        // Dev/default secrets are short strings; the derived key must still
        // be a valid HS256 key length (secret is SHA-256 hashed first).
        JwtService service = new JwtService(new JwtProperties("changeme", 60));

        String token = service.generateToken("admin@example.com", Role.ADMIN);

        assertThat(service.parse(token)).isPresent();
    }
}
