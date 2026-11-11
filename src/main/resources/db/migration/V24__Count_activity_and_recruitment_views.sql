ALTER TABLE external_activities ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE team_recruitments ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
