package CamNecT.server.domain.activity.repository.comment;

import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityCommentRepository extends JpaRepository<ActivityComment, Long> {

    @Query("select c.activity.activityId from ActivityComment c where c.id = :commentId")
    Optional<Long> findActivityIdByCommentId(@Param("commentId") Long commentId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select c from ActivityComment c where c.id = :commentId and c.status = :status")
    Optional<ActivityComment> findByIdAndStatusForRead(
            @Param("commentId") Long commentId,
            @Param("status") CommentStatus status
    );

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ActivityComment c where c.id = :commentId")
    Optional<ActivityComment> findByIdForUpdate(@Param("commentId") Long commentId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ActivityComment c where c.activity.activityId = :activityId order by c.id asc")
    List<ActivityComment> findAllByActivityIdForUpdate(@Param("activityId") Long activityId);

    @Query("""
        select c
        from ActivityComment c
        where c.activity.activityId = :activityId
          and c.parent is null
          and c.status in :statuses
          and (:cursorId is null or c.id < :cursorId)
        order by c.id desc
    """)
    List<ActivityComment> findRootPage(
            @Param("activityId") Long activityId,
            @Param("statuses") Collection<CommentStatus> statuses,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // Bound each root's preview in the database, while fetching all roots in one query.
    @Query(value = """
        select c.* from activity_comments c
        join (
            select comment_id,
                   row_number() over (partition by parent_comment_id order by created_at, comment_id) as reply_number
            from activity_comments
            where activity_id = :activityId and parent_comment_id in (:parentIds) and status in (:statuses)
        ) ranked on ranked.comment_id = c.comment_id
        where ranked.reply_number <= :limit
        order by c.parent_comment_id, c.created_at, c.comment_id
        """, nativeQuery = true)
    List<ActivityComment> findReplyPreviews(
            @Param("activityId") Long activityId,
            @Param("parentIds") Collection<Long> parentIds,
            @Param("statuses") Collection<String> statuses,
            @Param("limit") int limit
    );

    @Query("""
        select c from ActivityComment c
        where c.activity.activityId = :activityId and c.parent.id = :parentId and c.status in :statuses
          and (:cursorId is null or c.createdAt > :cursorCreatedAt
               or (c.createdAt = :cursorCreatedAt and c.id > :cursorId))
        order by c.createdAt, c.id
        """)
    List<ActivityComment> findReplyPage(
            @Param("activityId") Long activityId,
            @Param("parentId") Long parentId,
            @Param("statuses") Collection<CommentStatus> statuses,
            @Param("cursorId") Long cursorId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            Pageable pageable
    );

    // 게시글 삭제 시: 자기참조 FK를 지키기 위해 대댓글을 먼저 삭제한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ActivityComment c where c.activity.activityId = :activityId and c.parent is not null")
    void deleteRepliesByActivityId(@Param("activityId") Long activityId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ActivityComment c where c.activity.activityId = :activityId and c.parent is null")
    void deleteRootsByActivityId(@Param("activityId") Long activityId);

}
