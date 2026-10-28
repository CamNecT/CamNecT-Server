CREATE TABLE activity_imports (
    source_id VARCHAR(100) NOT NULL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    source_revision BIGINT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    managed_hash VARCHAR(64) NOT NULL,
    thumbnail_hash VARCHAR(64) NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_activity_import_activity (activity_id)
);
-- No FK: retain source identity after an administrator deletes the activity.
ALTER TABLE external_activities ADD COLUMN apply_end_at DATETIME(6) NULL;
