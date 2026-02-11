package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.TagRelation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

public interface TagGraphBatchRepository extends Repository<TagRelation, TagRelation.TagRelationId> {

    // DB 시간 기준 컷오프 (JVM-DB 시간 불일치 방지)
    @Query(value = "SELECT NOW(6)", nativeQuery = true)
    Timestamp now6();

    // -------------------------
    // 1) TagStats upsert (VALUES() 제거)
    // -------------------------

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT dt.context, dt.tag_id, dt.ins_doc_count, dt.ins_updated_at
        FROM (
            SELECT 'PROFILE' AS context,
                   utm.tag_id AS tag_id,
                   COUNT(DISTINCT utm.user_id) AS ins_doc_count,
                   :now AS ins_updated_at
            FROM user_tag_map utm
            JOIN tags t ON t.tag_id = utm.tag_id
            WHERE t.tag_category_id NOT IN (7,8)
              AND t.active = 1
            GROUP BY utm.tag_id
        ) dt
        ON DUPLICATE KEY UPDATE
          doc_count = ins_doc_count,
          updated_at = ins_updated_at
        """, nativeQuery = true)
    int upsertProfileTagStats(@Param("now") Timestamp now);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT dt.context, dt.tag_id, dt.ins_doc_count, dt.ins_updated_at
        FROM (
            SELECT 'POST_COMMUNITY' AS context,
                   pt.tag_id AS tag_id,
                   COUNT(DISTINCT pt.post_id) AS ins_doc_count,
                   :now AS ins_updated_at
            FROM post_tags pt
            JOIN posts p ON p.post_id = pt.post_id
            JOIN boards b ON b.board_id = p.board_id
            JOIN tags t ON t.tag_id = pt.tag_id
            WHERE b.code IN ('INFO','QUESTION')
              AND t.tag_category_id NOT IN (7,8)
              AND t.active = 1
            GROUP BY pt.tag_id
        ) dt
        ON DUPLICATE KEY UPDATE
          doc_count = ins_doc_count,
          updated_at = ins_updated_at
        """, nativeQuery = true)
    int upsertPostCommunityTagStats(@Param("now") Timestamp now);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_stats(context, tag_id, doc_count, updated_at)
        SELECT dt.context, dt.tag_id, dt.ins_doc_count, dt.ins_updated_at
        FROM (
            SELECT 'POST_ACTIVITY' AS context,
                   eat.tag_id AS tag_id,
                   COUNT(DISTINCT eat.activity_id) AS ins_doc_count,
                   :now AS ins_updated_at
            FROM external_activity_tags eat
            JOIN tags t ON t.tag_id = eat.tag_id
            WHERE t.tag_category_id NOT IN (7,8)
              AND t.active = 1
            GROUP BY eat.tag_id
        ) dt
        ON DUPLICATE KEY UPDATE
          doc_count = ins_doc_count,
          updated_at = ins_updated_at
        """, nativeQuery = true)
    int upsertPostActivityTagStats(@Param("now") Timestamp now);

    // -------------------------
    // 2) TagRelation upsert (VALUES() 제거 + updated_at=:now)
    // -------------------------

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
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
    SELECT
      'PROFILE' AS context,
      from_tag  AS from_tag_id,
      to_tag    AS to_tag_id,
      score     AS ins_score,
      co        AS ins_evidence_count,
      :now      AS ins_updated_at
    FROM ranked
    WHERE rn <= :topK
    ON DUPLICATE KEY UPDATE
      score = ins_score,
      evidence_count = ins_evidence_count,
      updated_at = ins_updated_at
    """, nativeQuery = true)
    int upsertProfileTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("now") java.sql.Timestamp now
    );

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
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
    SELECT
      'POST_COMMUNITY' AS context,
      from_tag AS from_tag_id,
      to_tag AS to_tag_id,
      score AS ins_score,
      co AS ins_evidence_count,
      :now AS ins_updated_at
    FROM ranked
    WHERE rn <= :topK
    ON DUPLICATE KEY UPDATE
      score = ins_score,
      evidence_count = ins_evidence_count,
      updated_at = ins_updated_at
    """, nativeQuery = true)
    int upsertPostCommunityTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("now") Timestamp now
    );

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO tag_relation(context, from_tag_id, to_tag_id, score, evidence_count, updated_at)
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
    SELECT
      'POST_ACTIVITY' AS context,
      from_tag AS from_tag_id,
      to_tag AS to_tag_id,
      score AS ins_score,
      co AS ins_evidence_count,
      :now AS ins_updated_at
    FROM ranked
    WHERE rn <= :topK
    ON DUPLICATE KEY UPDATE
      score = ins_score,
      evidence_count = ins_evidence_count,
      updated_at = ins_updated_at
    """, nativeQuery = true)
    int upsertPostActivityTagRelation(
            @Param("minEvidence") int minEvidence,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("now") Timestamp now
    );

    // -------------------------
    // 3) cleanup (cutoff=DB now6)
    // -------------------------

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM tag_relation
        WHERE context = 'PROFILE'
          AND updated_at < :cutoff
        """, nativeQuery = true)
    int cleanupProfileRelations(@Param("cutoff") Timestamp cutoff);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM tag_relation
        WHERE context = 'POST_COMMUNITY'
          AND updated_at < :cutoff
        """, nativeQuery = true)
    int cleanupPostCommunityRelations(@Param("cutoff") Timestamp cutoff);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM tag_relation
        WHERE context = 'POST_ACTIVITY'
          AND updated_at < :cutoff
        """, nativeQuery = true)
    int cleanupPostActivityRelations(@Param("cutoff") Timestamp cutoff);
}