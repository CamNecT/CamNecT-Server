CREATE TABLE post_viewers (
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_viewed_on DATE NOT NULL,
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
