-- Up to 10 entries of 50 characters plus 9 comma separators = 509 characters.
ALTER TABLE portfolio_project
    MODIFY COLUMN tech_stack VARCHAR(512) NOT NULL;
