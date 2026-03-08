CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NULL
);

CREATE TABLE user_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE workspace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_key VARCHAR(64) NOT NULL UNIQUE,
    ip_hash VARCHAR(128),
    ua_hash VARCHAR(128),
    owner_user_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_workspace_user FOREIGN KEY (owner_user_id) REFERENCES app_user(id)
);

CREATE TABLE project_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_project_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

CREATE TABLE generation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    prompt TEXT NOT NULL,
    component_name VARCHAR(80) NOT NULL,
    constraints_json TEXT,
    include_demo_data BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL,
    model VARCHAR(80) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_generation_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_generation_project FOREIGN KEY (project_id) REFERENCES project_space(id)
);

CREATE TABLE component_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    vue_code LONGTEXT NOT NULL,
    template_code LONGTEXT NOT NULL,
    script_code LONGTEXT NOT NULL,
    style_code LONGTEXT NOT NULL,
    safety_level VARCHAR(20) NOT NULL,
    safety_reason TEXT,
    compile_ok BOOLEAN NOT NULL,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_version_task FOREIGN KEY (task_id) REFERENCES generation_task(id),
    CONSTRAINT uk_task_version UNIQUE (task_id, version_no)
);

CREATE TABLE llm_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(80) NOT NULL,
    request_tokens INT NOT NULL,
    response_tokens INT NOT NULL,
    total_tokens INT NULL,
    estimated_cost_usd DOUBLE NULL,
    latency_ms BIGINT NOT NULL,
    finish_reason VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_llm_log_task FOREIGN KEY (task_id) REFERENCES generation_task(id)
);

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    target_user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_actor_user FOREIGN KEY (actor_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_audit_target_user FOREIGN KEY (target_user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_task_workspace_created ON generation_task (workspace_id, created_at DESC);
CREATE INDEX idx_task_project_created ON generation_task (project_id, created_at DESC);
CREATE INDEX idx_version_task_created ON component_version (task_id, created_at DESC);
CREATE INDEX idx_project_owner ON project_space (owner_user_id, updated_at DESC);
CREATE INDEX idx_audit_actor_created ON audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_created ON audit_log (created_at DESC);
