package com.example.uigen.repository;

import com.example.uigen.model.entity.Workspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByWorkspaceKey(String workspaceKey);

    Optional<Workspace> findByWorkspaceKeyAndOwnerUserId(String workspaceKey, Long ownerUserId);
}
