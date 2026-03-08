package com.example.uigen.version;

import com.example.uigen.safety.SafetyLevel;

import java.time.Instant;

public record ComponentVersionSummaryResponse(
        Long id,
        Long taskId,
        Integer versionNo,
        SafetyLevel safetyLevel,
        String safetyReason,
        boolean compileOk,
        Instant createdAt
) {
}
