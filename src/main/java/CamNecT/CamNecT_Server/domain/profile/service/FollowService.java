package CamNecT.CamNecT_Server.domain.profile.service;

import CamNecT.CamNecT_Server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.CamNecT_Server.domain.profile.dto.response.FollowListResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserFollow;
import CamNecT.CamNecT_Server.domain.users.repository.UserFollowRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new CustomException(UserErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        }

        if (!userRepository.existsById(followingId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        try {
            UserFollow follow = UserFollow.builder()
                    .followerId(followerId)
                    .followingId(followingId)
                    .build();
            followRepository.saveAndFlush(follow);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(UserErrorCode.ALREADY_FOLLOWING);
        }
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new CustomException(UserErrorCode.FOLLOW_NOT_FOUND);
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public FollowListResponse getFollowerList(Long userId) {
        List<UserFollow> follows = followRepository.findAllByFollowingId(userId);
        List<Long> followerIds = follows.stream()
                .map(UserFollow::getFollowerId)
                .toList();

        return getFollowListResponse(followerIds);
    }

    @Transactional(readOnly = true)
    public FollowListResponse getFollowingList(Long userId) {
        List<UserFollow> follows = followRepository.findAllByFollowerId(userId);
        List<Long> followingIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .toList();

        return getFollowListResponse(followingIds);
    }

    private FollowListResponse getFollowListResponse(List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return new FollowListResponse(List.of(), 0);
        }

        Map<Long, ProfileGlobalDto> globalMap =
                userProfileRepository.findGlobalsByUserIdIn(targetIds).stream()
                        .collect(Collectors.toMap(ProfileGlobalDto::userId, it -> it));

        List<FollowListResponse.FollowUserDetailDto> dtoList = targetIds.stream()
                .map(id -> {
                    ProfileGlobalDto g = globalMap.get(id);
                    if (g == null) return null;

                    String majorName = StringUtils.hasText(g.majorName()) ? g.majorName() : "전공 미입력";
                    String studentNo = StringUtils.hasText(g.studentNo()) ? g.studentNo() : "학번 미입력";

                    String imgUrl = "/images/default.png";
                    if (StringUtils.hasText(g.profileImageKey())) {
                        imgUrl = publicUrlIssuer.issuePublicUrl(g.profileImageKey());
                    }

                    return new FollowListResponse.FollowUserDetailDto(
                            id,
                            g.userName(),
                            majorName,
                            studentNo,
                            imgUrl
                    );
                })
                .filter(Objects::nonNull)
                .toList();


        return new FollowListResponse(dtoList, dtoList.size());
    }
}
