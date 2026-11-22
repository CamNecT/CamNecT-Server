ALTER TABLE report_evidence ADD COLUMN expired_at DATETIME(6);
CREATE INDEX idx_report_evidence_retention ON report_evidence (expired_at, created_at);
CREATE INDEX idx_report_penalty_retention ON user_report_penalty (created_at);
CREATE TABLE expired_penalty_summary (
    user_id BIGINT PRIMARY KEY,
    penalty_count BIGINT NOT NULL,
    permanently_banned BOOLEAN NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
