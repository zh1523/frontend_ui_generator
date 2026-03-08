package com.example.uigen.model.dto;

import java.time.Instant;

public record AdminAuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String action,
        Long targetUserId,
        String targetUsername,
        String detail,
        Instant createdAt
) {
}
