package com.example.uigen.repository;

import com.example.uigen.model.entity.GenerationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {
    Page<GenerationTask> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    Page<GenerationTask> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
}
