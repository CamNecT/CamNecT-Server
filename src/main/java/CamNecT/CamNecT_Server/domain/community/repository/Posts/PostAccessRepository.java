package CamNecT.CamNecT_Server.domain.community.repository.Posts;

import CamNecT.CamNecT_Server.domain.community.model.Posts.PostAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostAccessRepository extends JpaRepository<PostAccess, Long> {
    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostAccess pa where pa.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Query("""
        select pa.post.id
        from PostAccess pa
        where pa.user.userId = :userId
          and pa.post.id in :postIds
    """)
    List<Long> findGrantedPostIds(@Param("userId") Long userId,
                                  @Param("postIds") List<Long> postIds);
}
