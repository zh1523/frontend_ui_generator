package com.example.uigen.model.dto;

public record CostUsageResponse(
        long dailyTokenQuota,
        long usedTokens,
        long remainingTokens,
        double usedCostUsd
) {
}
