package com.example.uigen.version;

import com.example.uigen.common.ApiException;
import com.example.uigen.generation.GenerationService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ComponentVersionService {

    private final ComponentVersionRepository componentVersionRepository;
    private final GenerationService generationService;

    public ComponentVersionService(ComponentVersionRepository componentVersionRepository, GenerationService generationService) {
        this.componentVersionRepository = componentVersionRepository;
        this.generationService = generationService;
    }

    @Transactional(readOnly = true)
    public List<ComponentVersionSummaryResponse> listVersions(Long taskId, String workspaceKey) {
        generationService.requireTaskInWorkspace(taskId, workspaceKey);
        return componentVersionRepository.findByTaskIdOrderByVersionNoDesc(taskId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComponentVersionDetailResponse getVersionDetail(Long versionId, String workspaceKey) {
        ComponentVersion version = requireVersionInWorkspace(versionId, workspaceKey);
        return toDetail(version);
    }

    @Transactional
    public ResponseEntity<byte[]> downloadVersion(Long versionId, String workspaceKey) {
        ComponentVersion version = requireVersionInWorkspace(versionId, workspaceKey);
        version.setDownloadCount(version.getDownloadCount() + 1);
        String fileName = sanitizeFileName(version.getTask().getComponentName()) + "_v" + version.getVersionNo() + ".vue";
        byte[] content = version.getVueCode().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType("text/x-vue"))
                .body(content);
    }

    @Transactional(readOnly = true)
    public ComponentVersion requireVersionInWorkspace(Long versionId, String workspaceKey) {
        ComponentVersion version = componentVersionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(404, "Version not found"));
        generationService.requireTaskInWorkspace(version.getTask().getId(), workspaceKey);
        return version;
    }

    private ComponentVersionSummaryResponse toSummary(ComponentVersion version) {
        return new ComponentVersionSummaryResponse(
                version.getId(),
                version.getTask().getId(),
                version.getVersionNo(),
                version.getSafetyLevel(),
                version.getSafetyReason(),
                version.isCompileOk(),
                version.getCreatedAt()
        );
    }

    private ComponentVersionDetailResponse toDetail(ComponentVersion version) {
        return new ComponentVersionDetailResponse(
                version.getId(),
                version.getTask().getId(),
                version.getVersionNo(),
                version.getVueCode(),
                version.getTemplateCode(),
                version.getScriptCode(),
                version.getStyleCode(),
                version.getSafetyLevel(),
                version.getSafetyReason(),
                version.isCompileOk(),
                version.getCreatedAt()
        );
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
