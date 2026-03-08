package com.example.uigen.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_actor_created", columnList = "actor_user_id, created_at"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private AppUser actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private AppUser targetUser;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public void setActorUser(AppUser actorUser) {
        this.actorUser = actorUser;
    }

    public AppUser getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(AppUser targetUser) {
        this.targetUser = targetUser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
