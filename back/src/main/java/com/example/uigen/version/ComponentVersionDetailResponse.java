package com.example.uigen.version;

import com.example.uigen.safety.SafetyLevel;

import java.time.Instant;

public record ComponentVersionDetailResponse(
        Long id,
        Long taskId,
        Integer versionNo,
        String vueCode,
        String templateCode,
        String scriptCode,
        String styleCode,
        SafetyLevel safetyLevel,
        String safetyReason,
        boolean compileOk,
        Instant createdAt
) {
}
