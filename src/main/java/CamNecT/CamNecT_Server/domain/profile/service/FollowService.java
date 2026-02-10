package CamNecT.CamNecT_Server.domain.profile.service;

import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.domain.profile.dto.response.FollowListResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserFollow;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
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
    private final MajorRepository majorRepository;
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

        Map<Long, Users> userMap = userRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(Users::getUserId, u -> u));

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(targetIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Set<Long> majorIds = profileMap.values().stream()
                .map(UserProfile::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> majorNameMap = majorIds.isEmpty() ? Collections.emptyMap() :
                majorRepository.findAllById(majorIds).stream()
                        .collect(Collectors.toMap(
                                Majors::getMajorId,
                                m -> m.getMajorNameKor() != null ? m.getMajorNameKor() : "알 수 없는 전공"
                        ));

        List<FollowListResponse.FollowUserDetailDto> dtoList = targetIds.stream()
                .map(id -> {
                    Users user = userMap.get(id);
                    if (user == null) return null;

                    UserProfile profile = profileMap.get(id);

                    String majorName = "전공 미입력";
                    if (profile != null && profile.getMajorId() != null) {
                        majorName = majorNameMap.getOrDefault(profile.getMajorId(), "알 수 없는 전공");
                    }

                    String imgUrl = "/images/default.png";
                    if (profile != null && StringUtils.hasText(profile.getProfileImageKey())) {
                        imgUrl = publicUrlIssuer.issuePublicUrl(profile.getProfileImageKey());
                    }

                    return new FollowListResponse.FollowUserDetailDto(
                            id,
                            user.getName(),
                            majorName,
                            (profile != null && profile.getStudentNo() != null) ? profile.getStudentNo() : "학번 미입력",
                            imgUrl
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new FollowListResponse(dtoList, dtoList.size());
    }
}
