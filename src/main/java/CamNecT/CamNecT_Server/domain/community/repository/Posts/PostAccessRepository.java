package CamNecT.CamNecT_Server.domain.community.repository.Posts;

import CamNecT.CamNecT_Server.domain.community.model.Posts.PostAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostAccessRepository extends JpaRepository<PostAccess, Long> {
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostAccess pa where pa.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
