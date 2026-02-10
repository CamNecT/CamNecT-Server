package CamNecT.CamNecT_Server.domain.profile.service;

import CamNecT.CamNecT_Server.domain.users.model.UserFollow;
import CamNecT.CamNecT_Server.domain.users.repository.UserFollowRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new CustomException(UserErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        }

        if (!userRepository.existsById(followingId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new CustomException(UserErrorCode.ALREADY_FOLLOWING);
        }

        UserFollow follow = UserFollow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new CustomException(UserErrorCode.FOLLOW_NOT_FOUND);
        }
        
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }
}
