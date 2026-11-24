package CamNecT.server.domain.community.repository.Comments;

import CamNecT.server.domain.community.model.Comments.CommentLikes;
import CamNecT.server.domain.community.model.enums.CommentStatus;
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
    from CommentLikes cl join cl.comment c
    where c.post.id = :postId and c.status in :statuses
      and (c.id in :roots or c.parent.id in :roots)
    group by cl.comment.id
""")
    List<LikeCountRow> countForRootPage(@Param("postId") Long postId, @Param("roots") Collection<Long> roots,
                                       @Param("statuses") Collection<CommentStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentLikes cl where cl.comment.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
