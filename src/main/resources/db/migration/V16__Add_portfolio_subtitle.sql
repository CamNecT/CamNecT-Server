-- Existing projects have no subtitle; keep them NULL.
ALTER TABLE portfolio_project
    ADD COLUMN subtitle VARCHAR(50) NULL AFTER title;
