package CamNecT.CamNecT_Server.domain.profile.service;

import CamNecT.CamNecT_Server.domain.certificate.dto.response.CertificateResponse;
import CamNecT.CamNecT_Server.domain.certificate.repository.CertificateRepository;
import CamNecT.CamNecT_Server.domain.education.dto.response.EducationResponse;
import CamNecT.CamNecT_Server.domain.education.repository.EducationRepository;
import CamNecT.CamNecT_Server.domain.experience.dto.response.ExperienceResponse;
import CamNecT.CamNecT_Server.domain.experience.repository.ExperienceRepository;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.CamNecT_Server.domain.portfolio.repository.PortfolioRepository;
import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdateOnboardingRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdatePrivacyRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.request.UpdateProfileTagsRequest;
import CamNecT.CamNecT_Server.domain.profile.dto.response.ProfileStatusResponse;
import CamNecT.CamNecT_Server.domain.profile.dto.response.ProfileResponse;
import CamNecT.CamNecT_Server.domain.users.model.*;
import CamNecT.CamNecT_Server.domain.users.repository.*;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.service.DownloadUrlIssuer;
import CamNecT.CamNecT_Server.global.storage.service.FileStorage;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final ExperienceRepository experienceRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserFollowRepository userFollowRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserTagMapRepository userTagMapRepository;
    private final EducationRepository educationRepository;
    private final TagRepository tagRepository;
    private final PresignEngine presignEngine;
    private final FileStorage fileStorage;
    private final DownloadUrlIssuer downloadUrlIssuer;

    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(Long loginUserId, Long profileUserId) {

        Users user = userRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        UserProfile userProfile = userProfileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        String profileImageUrl = downloadUrlIssuer.issueDisplayUrl(userProfile.getProfileImageUrl());

        boolean isOwner = (loginUserId != null) && loginUserId.equals(profileUserId);
        boolean showFollower = isOwner || Boolean.TRUE.equals(userProfile.getIsFollowerVisible());

        int following = showFollower ? userFollowRepository.countByFollowingId(profileUserId) : 0;
        int follower = showFollower ? userFollowRepository.countByFollowerId(profileUserId) : 0;

        List<PortfolioPreviewResponse> portfolioPreviewResponses = portfolioRepository.findPreviewsByUserId(profileUserId);

        List<EducationResponse> educationResponses = educationRepository.findAllByUserIdWithDetails(profileUserId)
                .stream()
                .map(EducationResponse::from)
                .toList();
        List<ExperienceResponse> experienceList = experienceRepository.findAllByUser_UserIdOrderByStartDateDesc(profileUserId).stream()
                .map(ExperienceResponse::from)
                .toList();
        List<CertificateResponse> certificateList = certificateRepository.findAllByUser_UserIdOrderByAcquiredDateDesc(profileUserId).stream()
                .map(CertificateResponse::from)
                .toList();

        List<ProfileResponse.TagDto> tags = userTagMapRepository.findAllTagsByUserId(profileUserId).stream()
                .map(t -> new ProfileResponse.TagDto(t.getId(), t.getName(), t.getCategory(), t.getAttribute().getName()))
                .toList();

        ProfileResponse.ProfileBasicsDto basicProfile = new ProfileResponse.ProfileBasicsDto(
                userProfile.getBio(),
                userProfile.getOpenToCoffeeChat(),
                userProfile.getIsFollowerVisible(),
                profileImageUrl,
                userProfile.getStudentNo(),
                userProfile.getYearLevel(),
                userProfile.getInstitutionId(),
                userProfile.getMajorId()
        );

        return new ProfileResponse(
                user.getUserId(),
                user.getName(),
                basicProfile,
                following,
                follower,
                portfolioPreviewResponses,
                educationResponses,
                experienceList,
                certificateList,
                tags
        );
    }

    @Transactional
    public void updatePrivacy(Long userId, UpdatePrivacyRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        profile.updatePrivacySettings(
                request.isFollowerVisible(),
                request.isEducationVisible(),
                request.isExperienceVisible(),
                request.isCertificateVisible()
        );
    }

    @Transactional
    public ProfileStatusResponse updateBio(Long userId, String bio) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        userProfile.updateOnboardingProfile(bio, null);

        return new ProfileStatusResponse(userProfile.getUser().getStatus());
    }

    @Transactional
    public ProfileStatusResponse createOnboarding(Long userId, UpdateOnboardingRequest req) {

        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        requireEmailVerifiedAndNotSuspended(user);

        UserProfile userProfile = UserProfile.builder()
                .userId(userId)
                .user(user)
                .bio(null)
                .profileImageUrl(null)
                .openToCoffeeChat(false)
                .studentNo("TEMP")
                .yearLevel(1)
                .institutionId(1L)
                .majorId(1L)
                .build();

        userProfileRepository.save(userProfile);


        // 2) 프로필 이미지 key 처리 (presign temp -> final 승격)
        String finalProfileImageKey = null;
        if (req.profileImageKey() != null && !req.profileImageKey().isBlank()) {

            String finalPrefix = "profile/user-" + userId + "/images"; // final 위치(원하는 대로)
            finalProfileImageKey = presignEngine.consume(
                    userId,
                    UploadPurpose.PROFILE_IMAGE,
                    UploadRefType.USER_PROFILE,
                    userId, // refId는 userId로 두면 깔끔
                    req.profileImageKey(), // tempKey
                    finalPrefix
            );
        }

        userProfile.updateOnboardingProfile(req.bio(), finalProfileImageKey);

        // 3) 태그 replace
        List<Long> tagIds = (req.tagIds() == null) ? List.of() : req.tagIds().stream().distinct().toList();

        if (!tagIds.isEmpty()) {
            var tags = tagRepository.findAllById(tagIds);
            if (tags.size() != tagIds.size()) {
                throw new CustomException(UserErrorCode.INVALID_TAG_IDS);
            }
        }

        userTagMapRepository.deleteAllByUserId(userId);

        if (!tagIds.isEmpty()) {
            userTagMapRepository.saveAll(
                    tagIds.stream()
                            .map(tid -> UserTagMap.builder().userId(userId).tagId(tid).build())
                            .toList()
            );
        }

        return new ProfileStatusResponse(user.getStatus());
    }


    // =========================================================
    // 분야별 태그(관심분야 태그) 선택
    // =========================================================
    @Transactional
    public ProfileStatusResponse updateProfileTags(Long userId, UpdateProfileTagsRequest req) {

        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        requireEmailVerifiedAndNotSuspended(user);

        List<Long> tagIds = (req.tagIds() == null) ? List.of() : req.tagIds().stream().distinct().toList();

        // 존재 검증 (스킵 허용이면 empty OK)
        if (!tagIds.isEmpty()) {
            var tags = tagRepository.findAllById(tagIds);
            if (tags.size() != tagIds.size()) {
                throw new CustomException(UserErrorCode.INVALID_TAG_IDS);
            }
        }

        // 프로필 노출 태그 저장(user_tag_map)
        userTagMapRepository.deleteAllByUserId(userId);
        userTagMapRepository.saveAll(
                tagIds.stream()
                        .map(tid -> UserTagMap.builder().userId(userId).tagId(tid).build())
                        .toList()
        );
        return new ProfileStatusResponse(user.getStatus());
    }

    public PresignUploadResponse presignProfileImageUpload(Long userId, PresignUploadRequest req) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        requireEmailVerifiedAndNotSuspended(user);

        String ct = normalize(req.contentType());
        if (req.size() == null || req.size() <= 0) {
            throw new CustomException(StorageErrorCode.STORAGE_EMPTY_FILE);
        }

        String keyPrefix = "profile/user-" + userId + "/images";
        return presignEngine.issueUpload(
                userId,
                UploadPurpose.PROFILE_IMAGE,
                keyPrefix,
                ct,
                req.size(),
                req.originalFilename()
        );
    }

    private void requireEmailVerifiedAndNotSuspended(Users user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(UserErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.EMAIL_PENDING) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void deleteAfterCommit(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorage.delete(storageKey);
                    } catch (Exception ignored) {
                    }
                }
            });
        } else {
            try {
                fileStorage.delete(storageKey);
            } catch (Exception ignored) {
            }
        }
    }

    private String normalize(String ct) {
        return (ct == null) ? "" : ct.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }


}
