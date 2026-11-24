ALTER TABLE external_activities ADD COLUMN moderation_hidden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE team_recruitments ADD COLUMN moderation_hidden BOOLEAN NOT NULL DEFAULT FALSE;
