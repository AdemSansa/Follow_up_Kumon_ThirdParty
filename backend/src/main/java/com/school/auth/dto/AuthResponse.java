package com.school.auth.dto;

import com.school.auth.model.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
