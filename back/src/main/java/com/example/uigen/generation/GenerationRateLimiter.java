package com.example.uigen.generation;

import com.example.uigen.common.ApiException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class GenerationRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 8;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();

    public void checkOrThrow(String workspaceKey) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> deque = requestBuckets.computeIfAbsent(workspaceKey, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MILLIS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new ApiException(429, "Too many generation requests. Please retry later.");
            }
            deque.addLast(now);
        }
    }
}
