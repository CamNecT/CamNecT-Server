package CamNecT.server.domain.community.repository.Posts;

import CamNecT.server.domain.community.model.Posts.PostAttachments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostAttachmentsRepository extends JpaRepository<PostAttachments, Long> {

    // 상세: 첨부 전체(active만)
    List<PostAttachments> findByPost_IdAndStatusTrueOrderBySortOrderAscIdAsc(Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
       update PostAttachments a
          set a.status = false
        where a.post.id = :postId
          and a.status = true
       """)
    void softDeleteByPostId(@Param("postId") Long postId);

    Optional<PostAttachments> findByIdAndPost_IdAndStatusTrue(Long id, Long postId);

}

