package CamNecT.server.domain.community.repository.Posts;

import CamNecT.server.domain.community.model.Posts.PostBookmarks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostBookmarksRepository extends JpaRepository<PostBookmarks, Long> {

    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostBookmarks pb where pb.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
