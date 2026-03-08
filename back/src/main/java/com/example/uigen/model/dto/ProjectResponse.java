package com.example.uigen.model.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String workspaceKey,
        Instant createdAt,
        Instant updatedAt
) {
}
