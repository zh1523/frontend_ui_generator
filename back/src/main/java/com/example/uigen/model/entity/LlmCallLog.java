package com.example.uigen.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "llm_call_log")
public class LlmCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private GenerationTask task;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "request_tokens", nullable = false)
    private Integer requestTokens;

    @Column(name = "response_tokens", nullable = false)
    private Integer responseTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "finish_reason", length = 80)
    private String finishReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public GenerationTask getTask() {
        return task;
    }

    public void setTask(GenerationTask task) {
        this.task = task;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getRequestTokens() {
        return requestTokens;
    }

    public void setRequestTokens(Integer requestTokens) {
        this.requestTokens = requestTokens;
    }

    public Integer getResponseTokens() {
        return responseTokens;
    }

    public void setResponseTokens(Integer responseTokens) {
        this.responseTokens = responseTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public void setEstimatedCostUsd(Double estimatedCostUsd) {
        this.estimatedCostUsd = estimatedCostUsd;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
