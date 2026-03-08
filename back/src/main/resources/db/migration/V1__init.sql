CREATE TABLE workspace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_key VARCHAR(64) NOT NULL UNIQUE,
    ip_hash VARCHAR(128),
    ua_hash VARCHAR(128),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP NOT NULL
);

CREATE TABLE generation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    component_name VARCHAR(80) NOT NULL,
    constraints_json TEXT,
    status VARCHAR(20) NOT NULL,
    model VARCHAR(80) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_generation_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
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
    latency_ms BIGINT NOT NULL,
    finish_reason VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_llm_log_task FOREIGN KEY (task_id) REFERENCES generation_task(id)
);

CREATE INDEX idx_task_workspace_created ON generation_task (workspace_id, created_at DESC);
CREATE INDEX idx_version_task_created ON component_version (task_id, created_at DESC);
