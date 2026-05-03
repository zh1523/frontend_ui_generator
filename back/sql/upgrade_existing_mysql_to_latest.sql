-- MySQL 8.0+ incremental upgrade script
-- Purpose:
-- 1) Upgrade legacy uigen schema to latest auth/project/cost/cache-compatible schema
-- 2) Keep script idempotent so it can be executed safely more than once
--
-- Usage:
--   USE uigen;
--   SOURCE d:/java/biyesheji/back/sql/upgrade_existing_mysql_to_latest.sql;

USE uigen;

SET NAMES utf8mb4;

-- =========================================================
-- 1) Auth tables
-- =========================================================

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL;

UPDATE app_user
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS user_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_user_session_token ON user_session (token);
CREATE INDEX IF NOT EXISTS idx_user_session_user ON user_session (user_id);

-- add fk_session_user if missing
SET @exists_fk_session_user = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_session_user'
      AND TABLE_NAME = 'user_session'
);
SET @sql_fk_session_user = IF(
    @exists_fk_session_user = 0,
    'ALTER TABLE user_session ADD CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_session_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- 2) Workspace + Project
-- =========================================================

ALTER TABLE workspace
    ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_workspace_owner_user ON workspace (owner_user_id);

-- add fk_workspace_user if missing
SET @exists_fk_workspace_user = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_workspace_user'
      AND TABLE_NAME = 'workspace'
);
SET @sql_fk_workspace_user = IF(
    @exists_fk_workspace_user = 0,
    'ALTER TABLE workspace ADD CONSTRAINT fk_workspace_user FOREIGN KEY (owner_user_id) REFERENCES app_user(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_workspace_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS project_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_project_owner ON project_space (owner_user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_project_workspace ON project_space (workspace_id);

-- add fk_project_owner if missing
SET @exists_fk_project_owner = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_project_owner'
      AND TABLE_NAME = 'project_space'
);
SET @sql_fk_project_owner = IF(
    @exists_fk_project_owner = 0,
    'ALTER TABLE project_space ADD CONSTRAINT fk_project_owner FOREIGN KEY (owner_user_id) REFERENCES app_user(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_project_owner;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- add fk_project_workspace if missing
SET @exists_fk_project_workspace = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_project_workspace'
      AND TABLE_NAME = 'project_space'
);
SET @sql_fk_project_workspace = IF(
    @exists_fk_project_workspace = 0,
    'ALTER TABLE project_space ADD CONSTRAINT fk_project_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_project_workspace;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- 3) Generation task upgrade
-- =========================================================

ALTER TABLE generation_task
    ADD COLUMN IF NOT EXISTS project_id BIGINT NULL;

ALTER TABLE generation_task
    ADD COLUMN IF NOT EXISTS include_demo_data BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_task_project_created ON generation_task (project_id, created_at);

-- add fk_generation_project if missing
SET @exists_fk_generation_project = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_generation_project'
      AND TABLE_NAME = 'generation_task'
);
SET @sql_fk_generation_project = IF(
    @exists_fk_generation_project = 0,
    'ALTER TABLE generation_task ADD CONSTRAINT fk_generation_project FOREIGN KEY (project_id) REFERENCES project_space(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_generation_project;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- 4) LLM log upgrade (cost control fields)
-- =========================================================

ALTER TABLE llm_call_log
    ADD COLUMN IF NOT EXISTS total_tokens INT NULL;

ALTER TABLE llm_call_log
    ADD COLUMN IF NOT EXISTS estimated_cost_usd DOUBLE NULL;

-- backfill total_tokens for existing historical rows
UPDATE llm_call_log
SET total_tokens = COALESCE(request_tokens, 0) + COALESCE(response_tokens, 0)
WHERE total_tokens IS NULL;

-- =========================================================
-- 5) Admin audit log
-- =========================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    target_user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_audit_actor_created ON audit_log (actor_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log (created_at);

-- add fk_audit_actor_user if missing
SET @exists_fk_audit_actor_user = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_audit_actor_user'
      AND TABLE_NAME = 'audit_log'
);
SET @sql_fk_audit_actor_user = IF(
    @exists_fk_audit_actor_user = 0,
    'ALTER TABLE audit_log ADD CONSTRAINT fk_audit_actor_user FOREIGN KEY (actor_user_id) REFERENCES app_user(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_audit_actor_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- add fk_audit_target_user if missing
SET @exists_fk_audit_target_user = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_audit_target_user'
      AND TABLE_NAME = 'audit_log'
);
SET @sql_fk_audit_target_user = IF(
    @exists_fk_audit_target_user = 0,
    'ALTER TABLE audit_log ADD CONSTRAINT fk_audit_target_user FOREIGN KEY (target_user_id) REFERENCES app_user(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_fk_audit_target_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- 6) Post-check
-- =========================================================

SELECT 'upgrade_existing_mysql_to_latest.sql finished' AS message;
