package com.example.uigen.version;

import com.example.uigen.generation.GenerationTask;
import com.example.uigen.safety.SafetyLevel;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "component_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_task_version", columnNames = {"task_id", "version_no"}))
public class ComponentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private GenerationTask task;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "vue_code", nullable = false, columnDefinition = "LONGTEXT")
    private String vueCode;

    @Column(name = "template_code", nullable = false, columnDefinition = "LONGTEXT")
    private String templateCode;

    @Column(name = "script_code", nullable = false, columnDefinition = "LONGTEXT")
    private String scriptCode;

    @Column(name = "style_code", nullable = false, columnDefinition = "LONGTEXT")
    private String styleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_level", nullable = false, length = 20)
    private SafetyLevel safetyLevel;

    @Column(name = "safety_reason", columnDefinition = "TEXT")
    private String safetyReason;

    @Column(name = "compile_ok", nullable = false)
    private boolean compileOk;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getVueCode() {
        return vueCode;
    }

    public void setVueCode(String vueCode) {
        this.vueCode = vueCode;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public SafetyLevel getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(SafetyLevel safetyLevel) {
        this.safetyLevel = safetyLevel;
    }

    public String getSafetyReason() {
        return safetyReason;
    }

    public void setSafetyReason(String safetyReason) {
        this.safetyReason = safetyReason;
    }

    public boolean isCompileOk() {
        return compileOk;
    }

    public void setCompileOk(boolean compileOk) {
        this.compileOk = compileOk;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
