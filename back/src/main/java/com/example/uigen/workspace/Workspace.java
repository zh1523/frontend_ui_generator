package com.example.uigen.workspace;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "workspace")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_key", nullable = false, unique = true, length = 64)
    private String workspaceKey;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "ua_hash", length = 128)
    private String uaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    public Long getId() {
        return id;
    }

    public String getWorkspaceKey() {
        return workspaceKey;
    }

    public void setWorkspaceKey(String workspaceKey) {
        this.workspaceKey = workspaceKey;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getUaHash() {
        return uaHash;
    }

    public void setUaHash(String uaHash) {
        this.uaHash = uaHash;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
