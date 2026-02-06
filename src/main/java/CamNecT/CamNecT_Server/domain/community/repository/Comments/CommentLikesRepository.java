package CamNecT.CamNecT_Server.domain.community.repository.Comments;

import CamNecT.CamNecT_Server.domain.community.model.Comments.CommentLikes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentLikesRepository extends JpaRepository<CommentLikes, Long> {

    boolean existsByComment_IdAndUserId(Long commentId, Long userId);

    void deleteByComment_IdAndUserId(Long commentId, Long userId);

    long countByComment_Id(Long commentId);

    interface LikeCountRow {
        Long getCommentId();
        long getCnt();
    }

    @Query("""
    select cl.comment.id as commentId, count(cl) as cnt
    from CommentLikes cl
    where cl.comment.id in :commentIds
    group by cl.comment.id
""")
    List<LikeCountRow> countByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentLikes cl where cl.comment.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}