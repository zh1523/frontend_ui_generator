package com.example.uigen.controller;

import com.example.uigen.common.HttpHeadersConst;
import com.example.uigen.model.dto.ComponentVersionDetailResponse;
import com.example.uigen.model.dto.ComponentVersionSummaryResponse;
import com.example.uigen.service.ComponentVersionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ComponentVersionController {

    private final ComponentVersionService componentVersionService;

    public ComponentVersionController(ComponentVersionService componentVersionService) {
        this.componentVersionService = componentVersionService;
    }

    @GetMapping("/generations/{taskId}/versions")
    public List<ComponentVersionSummaryResponse> listVersions(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long taskId
    ) {
        return componentVersionService.listVersions(taskId, workspaceKey, projectId);
    }

    @GetMapping("/versions/{versionId}")
    public ComponentVersionDetailResponse getVersion(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long versionId
    ) {
        return componentVersionService.getVersionDetail(versionId, workspaceKey, projectId);
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<byte[]> downloadVersion(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long versionId
    ) {
        return componentVersionService.downloadVersion(versionId, workspaceKey, projectId);
    }
}
