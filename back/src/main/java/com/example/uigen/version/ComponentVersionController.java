package com.example.uigen.version;

import com.example.uigen.common.HttpHeadersConst;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @PathVariable Long taskId
    ) {
        return componentVersionService.listVersions(taskId, workspaceKey);
    }

    @GetMapping("/versions/{versionId}")
    public ComponentVersionDetailResponse getVersion(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @PathVariable Long versionId
    ) {
        return componentVersionService.getVersionDetail(versionId, workspaceKey);
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<byte[]> downloadVersion(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @PathVariable Long versionId
    ) {
        return componentVersionService.downloadVersion(versionId, workspaceKey);
    }
}
