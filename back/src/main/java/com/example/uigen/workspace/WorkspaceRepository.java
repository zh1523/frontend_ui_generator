package com.example.uigen.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByWorkspaceKey(String workspaceKey);
}
