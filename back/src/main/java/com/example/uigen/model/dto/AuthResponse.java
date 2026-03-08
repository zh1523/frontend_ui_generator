package com.example.uigen.model.dto;

import com.example.uigen.model.enums.UserRole;

import java.time.Instant;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        UserRole role,
        Instant expiresAt
) {
}
