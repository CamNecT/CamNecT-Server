package CamNecT.CamNecT_Server.domain.community.repository.Comments;

import CamNecT.CamNecT_Server.domain.community.model.Comments.Comments;
import CamNecT.CamNecT_Server.domain.community.model.enums.CommentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentsRepository extends JpaRepository<Comments, Long> {

    // 루트 댓글(부모=null) 조회 (최신순/오래된순은 취향)
    List<Comments> findByPost_IdAndParentIsNullAndStatusOrderByCreatedAtDesc(Long postId, CommentStatus status, Pageable pageable);

    // 루트 댓글 여러 개에 대한 답글을 한 번에 조회(부모 아래 created_at 정렬)
    List<Comments> findByPost_IdAndParent_IdInAndStatusOrderByParent_IdAscCreatedAtAsc(Long postId, Collection<Long> parentIds, CommentStatus status);

    // 게시글의 모든 댓글(상태 무관) - soft delete 정리, 통계용 등
    List<Comments> findByPost_Id(Long postId);

    // 대댓글 조회(부모 기준)
    List<Comments> findByPost_IdAndParent_IdAndStatusOrderByIdAsc(Long postId, Long parentCommentId, CommentStatus status);

    // 여러 postId의 댓글 수 집계가 필요할 때(나중에 피드용)
    long countByPost_IdAndStatus(Long postId, CommentStatus status);

    // postIds 묶어서 가져오기(피드에서 댓글 미리보기 같은 것 할 때)
    List<Comments> findByPost_IdInAndStatus(Collection<Long> postIds, CommentStatus status);

    // 게시글 삭제 시: 댓글 하드 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comments c where c.post.id = :postId")
    int deleteByPostId(@Param("postId") Long postId);

}
