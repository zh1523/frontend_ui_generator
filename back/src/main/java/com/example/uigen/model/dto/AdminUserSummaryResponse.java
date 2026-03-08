package com.example.uigen.model.dto;

import com.example.uigen.model.enums.UserRole;

import java.time.Instant;

public record AdminUserSummaryResponse(
        Long userId,
        String username,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {
}
