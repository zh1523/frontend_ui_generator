package com.example.uigen.service;

import com.example.uigen.config.AppProperties;
import com.example.uigen.model.dto.SafetyScanResult;
import com.example.uigen.model.dto.SfcSections;
import com.example.uigen.model.entity.GenerationTask;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GenerationCacheService {

    private final AppProperties appProperties;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GenerationCacheService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public CachedGeneration get(GenerationTask task) {
        String key = buildKey(task);
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    public void put(GenerationTask task, SfcSections sections, SafetyScanResult safetyScanResult) {
        String key = buildKey(task);
        int ttlMinutes = resolveTtlMinutes();
        cache.put(key, new CacheEntry(
                new CachedGeneration(sections, safetyScanResult),
                Instant.now().plusSeconds(ttlMinutes * 60L)
        ));
    }

    public String buildKey(GenerationTask task) {
        String raw = task.getModel() + "|" +
                task.getComponentName() + "|" +
                task.getPrompt() + "|" +
                task.getConstraintsJson() + "|" +
                task.isIncludeDemoData();
        return sha256(raw);
    }

    private int resolveTtlMinutes() {
        AppProperties.Generation generation = appProperties.generation();
        if (generation == null || generation.cacheTtlMinutes() == null || generation.cacheTtlMinutes() <= 0) {
            return 30;
        }
        return generation.cacheTtlMinutes();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private record CacheEntry(CachedGeneration value, Instant expiresAt) {
    }

    public record CachedGeneration(SfcSections sections, SafetyScanResult safetyScanResult) {
    }
}
