package com.example.uigen.generation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {
    Page<GenerationTask> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);
}
