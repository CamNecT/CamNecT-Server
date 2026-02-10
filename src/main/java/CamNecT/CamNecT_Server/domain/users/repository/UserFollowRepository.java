package CamNecT.CamNecT_Server.domain.users.repository;

import CamNecT.CamNecT_Server.domain.users.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    // 내가 팔로우하는 사람 수 (팔로잉)
    int countByFollowerId(Long userId);

    // 나를 팔로우하는 사람 수 (팔로워)
    int countByFollowingId(Long userId);

    //팔로잉 여부 조회
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 팔로우 취소 (언팔)
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<UserFollow> findAllByFollowingId(Long userId);

    List<UserFollow> findAllByFollowerId(Long userId);
}
