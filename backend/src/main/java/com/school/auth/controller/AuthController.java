package com.school.auth.controller;

import com.school.auth.dto.AuthResponse;
import com.school.auth.dto.LoginRequest;
import com.school.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Reissues a fresh access token for the caller's current, still-valid
     * token. There is no server-side refresh-token store in the MVP —
     * stateless JWT only.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(Authentication authentication) {
        return ResponseEntity.ok(authService.refresh(authentication.getName()));
    }

    /**
     * Stateless JWT has no server-side session to invalidate; this exists
     * so the frontend has a symmetric endpoint to call while discarding its
     * local token. Revocation/blacklisting can be added later if needed.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
