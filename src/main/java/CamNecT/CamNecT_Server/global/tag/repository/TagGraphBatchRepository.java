package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.TagRelation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TagGraphBatchRepository extends Repository<TagRelation, TagRelation.TagRelationId> {

    // -------------------------
    // 1) TagStats upsert
    // -------------------------

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT 'PROFILE', utm.tag_id, COUNT(DISTINCT utm.user_id), NOW(6)
        FROM user_tag_map utm
        JOIN tags t ON t.tag_id = utm.tag_id
        WHERE t.tag_category_id NOT IN (7,8)
          AND t.active = 1
        GROUP BY utm.tag_id
        ON DUPLICATE KEY UPDATE
          doc_count = VALUES(doc_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertProfileTagStats();

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT 'POST_COMMUNITY', pt.tag_id, COUNT(DISTINCT pt.post_id), NOW(6)
        FROM post_tags pt
        JOIN posts p ON p.post_id = pt.post_id
        JOIN boards b ON b.board_id = p.board_id
        JOIN tags t ON t.tag_id = pt.tag_id
        WHERE b.code IN ('INFO','QUESTION')
          AND t.tag_category_id NOT IN (7,8)
          AND t.active = 1
        GROUP BY pt.tag_id
        ON DUPLICATE KEY UPDATE
          doc_count = VALUES(doc_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertPostCommunityTagStats();

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT 'POST_ACTIVITY', eat.tag_id, COUNT(DISTINCT eat.activity_id), NOW(6)
        FROM external_activity_tags eat
        JOIN tags t ON t.tag_id = eat.tag_id
        WHERE t.tag_category_id NOT IN (7,8)
          AND t.active = 1
        GROUP BY eat.tag_id
        ON DUPLICATE KEY UPDATE
          doc_count = VALUES(doc_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertPostActivityTagStats();

    // -------------------------
    // 2) TagRelation upsert (Cosine + minEvidence + threshold + TopK)
    // -------------------------

    // PROFILE
    @Modifying
    @Transactional
    @Query(value = """
        WITH
        tag_cnt AS (
          SELECT utm.tag_id, COUNT(DISTINCT utm.user_id) AS cnt
          FROM user_tag_map utm
          JOIN tags t ON t.tag_id = utm.tag_id
          WHERE t.tag_category_id NOT IN (7,8)
            AND t.active = 1
          GROUP BY utm.tag_id
        ),
        pairs AS (
          SELECT a.tag_id AS tag_a,
                 b.tag_id AS tag_b,
                 COUNT(DISTINCT a.user_id) AS co
          FROM user_tag_map a
          JOIN tags ta ON ta.tag_id = a.tag_id
          JOIN user_tag_map b
            ON a.user_id = b.user_id
           AND a.tag_id < b.tag_id
          JOIN tags tb ON tb.tag_id = b.tag_id
          WHERE ta.tag_category_id NOT IN (7,8)
            AND tb.tag_category_id NOT IN (7,8)
            AND ta.active = 1
            AND tb.active = 1
          GROUP BY a.tag_id, b.tag_id
          HAVING co >= :minEvidence
        ),
        scored AS (
          SELECT tag_a, tag_b, co,
                 (co / SQRT(tc1.cnt * tc2.cnt)) AS score
          FROM pairs
          JOIN tag_cnt tc1 ON tc1.tag_id = pairs.tag_a
          JOIN tag_cnt tc2 ON tc2.tag_id = pairs.tag_b
          WHERE (co / SQRT(tc1.cnt * tc2.cnt)) >= :threshold
        ),
        directed AS (
          SELECT tag_a AS from_tag, tag_b AS to_tag, score, co FROM scored
          UNION ALL
          SELECT tag_b AS from_tag, tag_a AS to_tag, score, co FROM scored
        ),
        ranked AS (
          SELECT *,
                 ROW_NUMBER() OVER (PARTITION BY from_tag ORDER BY score DESC, co DESC, to_tag) AS rn
          FROM directed
        )
        INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
        SELECT 'PROFILE', from_tag, to_tag, score, co, NOW(6)
        FROM ranked
        WHERE rn <= :topK
        ON DUPLICATE KEY UPDATE
          score = VALUES(score),
          evidence_count = VALUES(evidence_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertProfileTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK
    );

    // POST_COMMUNITY
    @Modifying
    @Transactional
    @Query(value = """
        WITH
        doc_tags AS (
          SELECT pt.post_id AS doc_id, pt.tag_id
          FROM post_tags pt
          JOIN posts p ON p.post_id = pt.post_id
          JOIN boards b ON b.board_id = p.board_id
          JOIN tags t ON t.tag_id = pt.tag_id
          WHERE b.code IN ('INFO','QUESTION')
            AND t.tag_category_id NOT IN (7,8)
            AND t.active = 1
        ),
        tag_cnt AS (
          SELECT tag_id, COUNT(DISTINCT doc_id) AS cnt
          FROM doc_tags
          GROUP BY tag_id
        ),
        pairs AS (
          SELECT a.tag_id AS tag_a,
                 b.tag_id AS tag_b,
                 COUNT(DISTINCT a.doc_id) AS co
          FROM doc_tags a
          JOIN doc_tags b
            ON a.doc_id = b.doc_id
           AND a.tag_id < b.tag_id
          GROUP BY a.tag_id, b.tag_id
          HAVING co >= :minEvidence
        ),
        scored AS (
          SELECT tag_a, tag_b, co,
                 (co / SQRT(tc1.cnt * tc2.cnt)) AS score
          FROM pairs
          JOIN tag_cnt tc1 ON tc1.tag_id = pairs.tag_a
          JOIN tag_cnt tc2 ON tc2.tag_id = pairs.tag_b
          WHERE (co / SQRT(tc1.cnt * tc2.cnt)) >= :threshold
        ),
        directed AS (
          SELECT tag_a AS from_tag, tag_b AS to_tag, score, co FROM scored
          UNION ALL
          SELECT tag_b AS from_tag, tag_a AS to_tag, score, co FROM scored
        ),
        ranked AS (
          SELECT *,
                 ROW_NUMBER() OVER (PARTITION BY from_tag ORDER BY score DESC, co DESC, to_tag) AS rn
          FROM directed
        )
        INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
        SELECT 'POST_COMMUNITY', from_tag, to_tag, score, co, NOW(6)
        FROM ranked
        WHERE rn <= :topK
        ON DUPLICATE KEY UPDATE
          score = VALUES(score),
          evidence_count = VALUES(evidence_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertPostCommunityTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK
    );

    // POST_ACTIVITY
    @Modifying
    @Transactional
    @Query(value = """
        WITH
        tag_cnt AS (
          SELECT eat.tag_id, COUNT(DISTINCT eat.activity_id) AS cnt
          FROM external_activity_tags eat
          JOIN tags t ON t.tag_id = eat.tag_id
          WHERE t.tag_category_id NOT IN (7,8)
            AND t.active = 1
          GROUP BY eat.tag_id
        ),
        pairs AS (
          SELECT a.tag_id AS tag_a,
                 b.tag_id AS tag_b,
                 COUNT(DISTINCT a.activity_id) AS co
          FROM external_activity_tags a
          JOIN tags ta ON ta.tag_id = a.tag_id
          JOIN external_activity_tags b
            ON a.activity_id = b.activity_id
           AND a.tag_id < b.tag_id
          JOIN tags tb ON tb.tag_id = b.tag_id
          WHERE ta.tag_category_id NOT IN (7,8)
            AND tb.tag_category_id NOT IN (7,8)
            AND ta.active = 1
            AND tb.active = 1
          GROUP BY a.tag_id, b.tag_id
          HAVING co >= :minEvidence
        ),
        scored AS (
          SELECT tag_a, tag_b, co,
                 (co / SQRT(tc1.cnt * tc2.cnt)) AS score
          FROM pairs
          JOIN tag_cnt tc1 ON tc1.tag_id = pairs.tag_a
          JOIN tag_cnt tc2 ON tc2.tag_id = pairs.tag_b
          WHERE (co / SQRT(tc1.cnt * tc2.cnt)) >= :threshold
        ),
        directed AS (
          SELECT tag_a AS from_tag, tag_b AS to_tag, score, co FROM scored
          UNION ALL
          SELECT tag_b AS from_tag, tag_a AS to_tag, score, co FROM scored
        ),
        ranked AS (
          SELECT *,
                 ROW_NUMBER() OVER (PARTITION BY from_tag ORDER BY score DESC, co DESC, to_tag) AS rn
          FROM directed
        )
        INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
        SELECT 'POST_ACTIVITY', from_tag, to_tag, score, co, NOW(6)
        FROM ranked
        WHERE rn <= :topK
        ON DUPLICATE KEY UPDATE
          score = VALUES(score),
          evidence_count = VALUES(evidence_count),
          updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    int upsertPostActivityTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK
    );

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM tag_relation
    WHERE context = 'PROFILE'
      AND updated_at < :cutoff
    """, nativeQuery = true)
    int cleanupProfileRelations(@Param("cutoff") java.sql.Timestamp cutoff);

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM tag_relation
    WHERE context = 'POST_COMMUNITY'
      AND updated_at < :cutoff
    """, nativeQuery = true)
    int cleanupPostCommunityRelations(@Param("cutoff") java.sql.Timestamp cutoff);

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM tag_relation
    WHERE context = 'POST_ACTIVITY'
      AND updated_at < :cutoff
    """, nativeQuery = true)
    int cleanupPostActivityRelations(@Param("cutoff") java.sql.Timestamp cutoff);
}