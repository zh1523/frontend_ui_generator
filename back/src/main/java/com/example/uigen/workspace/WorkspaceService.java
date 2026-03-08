package com.example.uigen.workspace;

import com.example.uigen.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public CreateWorkspaceResponse createAnonymousWorkspace(String ipHash, String uaHash) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceKey(UUID.randomUUID().toString().replace("-", ""));
        workspace.setIpHash(ipHash);
        workspace.setUaHash(uaHash);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setCreatedAt(Instant.now());
        workspace.setLastActiveAt(Instant.now());
        workspaceRepository.save(workspace);
        return new CreateWorkspaceResponse(workspace.getWorkspaceKey());
    }

    @Transactional
    public Workspace requireActiveWorkspace(String workspaceKey) {
        if (workspaceKey == null || workspaceKey.isBlank()) {
            throw new ApiException(400, "Missing X-Workspace-Key");
        }
        Workspace workspace = workspaceRepository.findByWorkspaceKey(workspaceKey)
                .orElseThrow(() -> new ApiException(404, "Workspace not found"));
        if (workspace.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ApiException(403, "Workspace is disabled");
        }
        workspace.setLastActiveAt(Instant.now());
        return workspace;
    }
}
