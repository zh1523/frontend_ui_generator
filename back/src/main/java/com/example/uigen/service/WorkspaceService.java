package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.model.dto.CreateWorkspaceResponse;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.entity.ProjectSpace;
import com.example.uigen.model.entity.Workspace;
import com.example.uigen.model.enums.WorkspaceStatus;
import com.example.uigen.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public CreateWorkspaceResponse createAnonymousWorkspace(String ipHash, String uaHash) {
        Workspace workspace = createWorkspaceEntity(null, ipHash, uaHash);
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

    @Transactional
    public Workspace createWorkspaceForUser(AppUser user, String ipHash, String uaHash) {
        Workspace workspace = createWorkspaceEntity(user, ipHash, uaHash);
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public Workspace requireWorkspaceForProject(String workspaceKey, ProjectSpace project, Long userId) {
        Workspace workspace = requireActiveWorkspace(workspaceKey);
        if (project == null || project.getWorkspace() == null) {
            throw new ApiException(400, "Project workspace is missing");
        }
        if (!project.getWorkspace().getId().equals(workspace.getId())) {
            throw new ApiException(403, "Workspace does not belong to current project");
        }
        if (!project.getOwnerUser().getId().equals(userId)) {
            throw new ApiException(403, "Project is not in current user");
        }
        return workspace;
    }

    private Workspace createWorkspaceEntity(AppUser owner, String ipHash, String uaHash) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceKey(UUID.randomUUID().toString().replace("-", ""));
        workspace.setIpHash(ipHash);
        workspace.setUaHash(uaHash);
        workspace.setOwnerUser(owner);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setCreatedAt(Instant.now());
        workspace.setLastActiveAt(Instant.now());
        return workspace;
    }
}
