CREATE TABLE community_reward_pairs (
    source_type VARCHAR(50) NOT NULL,
    low_user_id BIGINT NOT NULL,
    high_user_id BIGINT NOT NULL,
    rewarded_at DATETIME(6),
    PRIMARY KEY (source_type, low_user_id, high_user_id)
);

-- The question owner and paid answerer are recorded even when the accepted comment later changes.
INSERT INTO community_reward_pairs (source_type, low_user_id, high_user_id, rewarded_at)
SELECT t.source_type, LEAST(p.user_id, t.user_id), GREATEST(p.user_id, t.user_id), MAX(t.created_at)
FROM point_transaction t JOIN posts p ON p.post_id = t.post_id
WHERE t.source_type = 'COMMENT_SELECTION' AND t.transaction_type = 'EARN' AND t.created_at > NOW() - INTERVAL 30 DAY
GROUP BY t.source_type, LEAST(p.user_id, t.user_id), GREATEST(p.user_id, t.user_id);
