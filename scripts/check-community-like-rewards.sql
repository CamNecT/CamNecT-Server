-- Read-only pre-deployment check. Each row identifies a post to inspect.
WITH likes AS (
    SELECT post_id, COUNT(*) AS actual_likes
    FROM post_likes
    GROUP BY post_id
), rewards AS (
    SELECT post_id, COUNT(*) AS grants, MIN(user_id) AS recipient
    FROM point_transaction
    WHERE source_type = 'THREELIKES_REWARD' AND transaction_type = 'EARN'
    GROUP BY post_id
)
SELECT p.post_id, s.like_count AS cached_likes, COALESCE(l.actual_likes, 0) AS actual_likes,
       CASE WHEN s.like_rewarded_3 THEN 1 ELSE 0 END AS marked_paid,
       COALESCE(r.grants, 0) AS ledger_grants
FROM posts p
LEFT JOIN post_stats s ON s.post_id = p.post_id
LEFT JOIN likes l ON l.post_id = p.post_id
LEFT JOIN rewards r ON r.post_id = p.post_id
WHERE s.post_id IS NULL OR s.like_count <> COALESCE(l.actual_likes, 0)
   OR COALESCE(r.grants, 0) <> CASE WHEN s.like_rewarded_3 THEN 1 ELSE 0 END
   OR r.recipient <> p.user_id
ORDER BY p.post_id;
