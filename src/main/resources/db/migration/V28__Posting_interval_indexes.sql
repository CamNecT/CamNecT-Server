CREATE INDEX idx_comments_author_time ON comments (user_id, created_at);
CREATE INDEX idx_activity_comments_author_time ON activity_comments (user_id, created_at);
CREATE INDEX idx_posts_author_time ON posts (user_id, created_at);
