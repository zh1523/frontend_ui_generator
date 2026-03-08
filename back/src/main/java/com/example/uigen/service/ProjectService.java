package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.common.AuthContextHolder;
import com.example.uigen.model.dto.CreateProjectRequest;
import com.example.uigen.model.dto.ProjectResponse;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.entity.ProjectSpace;
import com.example.uigen.model.entity.Workspace;
import com.example.uigen.repository.ProjectSpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectSpaceRepository projectSpaceRepository;
    private final WorkspaceService workspaceService;

    public ProjectService(ProjectSpaceRepository projectSpaceRepository, WorkspaceService workspaceService) {
        this.projectSpaceRepository = projectSpaceRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        AppUser user = requireCurrentUser();
        Workspace workspace = workspaceService.createWorkspaceForUser(user, "", "");
        ProjectSpace project = new ProjectSpace();
        project.setOwnerUser(user);
        project.setWorkspace(workspace);
        project.setName(request.name().trim());
        project.setDescription((request.description() == null || request.description().isBlank()) ? "" : request.description().trim());
        project.setArchived(false);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        projectSpaceRepository.save(project);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMyProjects() {
        Long userId = requireCurrentUserId();
        return projectSpaceRepository.findByOwnerUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectSpace requireMyProject(Long projectId) {
        Long userId = requireCurrentUserId();
        return projectSpaceRepository.findByIdAndOwnerUserIdAndArchivedFalse(projectId, userId)
                .orElseThrow(() -> new ApiException(404, "Project not found"));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        return toResponse(requireMyProject(projectId));
    }

    private ProjectResponse toResponse(ProjectSpace project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getWorkspace().getWorkspaceKey(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private Long requireCurrentUserId() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            throw new ApiException(401, "Unauthorized");
        }
        return userId;
    }

    private AppUser requireCurrentUser() {
        AppUser user = AuthContextHolder.getUser();
        if (user == null) {
            throw new ApiException(401, "Unauthorized");
        }
        return user;
    }
}
