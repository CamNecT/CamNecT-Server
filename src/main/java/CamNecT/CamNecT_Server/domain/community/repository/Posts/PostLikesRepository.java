package CamNecT.CamNecT_Server.domain.community.repository.Posts;

import CamNecT.CamNecT_Server.domain.community.model.Posts.PostLikes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikesRepository extends JpaRepository<PostLikes, Long> {

    boolean existsByPost_IdAndUser_UserId(Long postId, Long userId);

    void deleteByPost_IdAndUser_UserId(Long postId, Long userId);
}