CREATE TABLE activity_comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    status ENUM('DELETED', 'HIDDEN', 'PUBLISHED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_activity_comments_activity FOREIGN KEY (activity_id) REFERENCES external_activities (activity_id),
    CONSTRAINT fk_activity_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES activity_comments (comment_id),
    INDEX idx_activity_comments_root_cursor (activity_id, parent_comment_id, comment_id, status),
    INDEX idx_activity_comments_children (activity_id, parent_comment_id, status, created_at, comment_id)
) ENGINE=InnoDB;

CREATE TABLE activity_comment_likes (
    comment_like_id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (comment_like_id),
    CONSTRAINT fk_activity_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES activity_comments (comment_id),
    CONSTRAINT uk_activity_comment_likes_comment_user UNIQUE (comment_id, user_id),
    INDEX idx_activity_comment_likes_comment (comment_id),
    INDEX idx_activity_comment_likes_user (user_id)
) ENGINE=InnoDB;
