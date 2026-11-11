package CamNecT.server.domain.activity.repository.comment;

import CamNecT.server.domain.activity.model.comment.ActivityCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityCommentLikeRepository extends JpaRepository<ActivityCommentLike, Long> {

    boolean existsByComment_IdAndUserId(Long commentId, Long userId);

    void deleteByComment_IdAndUserId(Long commentId, Long userId);

    long countByComment_Id(Long commentId);

    interface LikeCountRow {
        Long getCommentId();
        long getCnt();
    }

    @Query("""
    select cl.comment.id as commentId, count(cl) as cnt
    from ActivityCommentLike cl
    where cl.comment.id in :commentIds
    group by cl.comment.id
""")
    List<LikeCountRow> countByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ActivityCommentLike cl where cl.comment.activity.activityId = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}