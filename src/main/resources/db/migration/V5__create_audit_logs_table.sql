CREATE TABLE audit_logs (
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT,
        action VARCHAR(255) NOT NULL,
        entity_type VARCHAR(100),
        entity_id BIGINT,
        details TEXT,
        ip_address VARCHAR(50),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);