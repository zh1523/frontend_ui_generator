package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.config.AppProperties;
import com.example.uigen.model.dto.CostUsageResponse;
import com.example.uigen.repository.LlmCallLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CostControlService {

    private final AppProperties appProperties;
    private final LlmCallLogRepository llmCallLogRepository;
    private final ConcurrentHashMap<String, UsageSnapshot> usageCache = new ConcurrentHashMap<>();

    public CostControlService(AppProperties appProperties, LlmCallLogRepository llmCallLogRepository) {
        this.appProperties = appProperties;
        this.llmCallLogRepository = llmCallLogRepository;
    }

    public void checkQuotaOrThrow(Long userId) {
        CostUsageResponse usage = getCurrentUsage(userId);
        if (usage.remainingTokens() <= 0) {
            throw new ApiException(429, "Daily token quota exceeded");
        }
    }

    public CostUsageResponse getCurrentUsage(Long userId) {
        long quota = resolveDailyTokenQuota();
        UsageSnapshot snapshot = loadOrRefresh(userId);
        long remaining = Math.max(0, quota - snapshot.usedTokens());
        return new CostUsageResponse(quota, snapshot.usedTokens(), remaining, snapshot.usedCostUsd());
    }

    public void recordUsage(Long userId, int totalTokens, double costUsd) {
        String key = cacheKey(userId, LocalDate.now(ZoneOffset.UTC));
        usageCache.compute(key, (ignored, old) -> {
            if (old == null) {
                return new UsageSnapshot(totalTokens, costUsd, Instant.now().plusSeconds(60));
            }
            return new UsageSnapshot(old.usedTokens() + totalTokens, old.usedCostUsd() + costUsd, Instant.now().plusSeconds(60));
        });
    }

    public double estimateCostUsd(int requestTokens, int responseTokens) {
        AppProperties.Cost cost = appProperties.cost();
        if (cost == null) {
            return 0.0;
        }
        double inputPrice = cost.inputCostPer1kTokenUsd() == null ? 0.0 : cost.inputCostPer1kTokenUsd();
        double outputPrice = cost.outputCostPer1kTokenUsd() == null ? 0.0 : cost.outputCostPer1kTokenUsd();
        return (requestTokens / 1000.0) * inputPrice + (responseTokens / 1000.0) * outputPrice;
    }

    private UsageSnapshot loadOrRefresh(Long userId) {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String key = cacheKey(userId, date);
        UsageSnapshot current = usageCache.get(key);
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current;
        }
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long usedTokens = llmCallLogRepository.sumTotalTokensByUserAndTimeRange(userId, start, end);
        double usedCost = llmCallLogRepository.sumCostByUserAndTimeRange(userId, start, end);
        UsageSnapshot snapshot = new UsageSnapshot(usedTokens, usedCost, Instant.now().plusSeconds(60));
        usageCache.put(key, snapshot);
        return snapshot;
    }

    private long resolveDailyTokenQuota() {
        AppProperties.Cost cost = appProperties.cost();
        if (cost == null || cost.dailyTokenQuota() == null || cost.dailyTokenQuota() <= 0) {
            return 100_000L;
        }
        return cost.dailyTokenQuota();
    }

    private String cacheKey(Long userId, LocalDate date) {
        return userId + ":" + date;
    }

    private record UsageSnapshot(long usedTokens, double usedCostUsd, Instant expiresAt) {
    }
}
