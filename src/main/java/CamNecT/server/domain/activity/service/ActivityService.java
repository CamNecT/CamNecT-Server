package CamNecT.server.domain.activity.service;

import CamNecT.server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.server.domain.activity.dto.ExternalActivityDto;
import CamNecT.server.domain.activity.dto.TeamRecruitmentDto;
import CamNecT.server.domain.activity.dto.request.ActivityRequest;
import CamNecT.server.domain.activity.dto.request.AdminActivityRequest;
import CamNecT.server.domain.activity.dto.response.ActivityDetailResponse;
import CamNecT.server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.enums.ActivityStatus;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivityAttachment;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivityBookmark;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivityTag;
import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityAttachmentRepository;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityBookmarkRepository;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityTagRepository;
import CamNecT.server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.community.service.AuthorAssembler;
import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.server.global.common.service.GlobalPresignMethods;
import CamNecT.server.global.storage.model.UploadPurpose;
import CamNecT.server.global.storage.model.UploadRefType;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.PresignEngine;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import CamNecT.server.global.tag.model.Tag;
import CamNecT.server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityService {

    private static final String DEFAULT_THUMB = "기본이미지";

    private final ExternalActivityRepository activityRepository;
    private final ExternalActivityTagRepository activityTagRepository;
    private final ExternalActivityAttachmentRepository activityAttachmentRepository;
    private final ExternalActivityBookmarkRepository activityBookmarkRepository;
    private final TagRepository tagRepository;
    private final TeamRecruitmentRepository teamRecruitmentRepository;
    private final UserRepository userRepository;

    private final AuthorAssembler authorAssembler;

    //S3 관련 의존성 주입
    private final UploadTicketRepository uploadTicketRepository;
    private final PresignEngine presignEngine;
    private final PublicUrlIssuer publicUrlIssuer;
    private final GlobalPresignMethods globalPresignMethods;

    public Slice<ActivityPreviewResponse> getActivities(
            Long userId,
            ActivityCategory category,
            List<Long> tagIds,
            String title,
            String sortType,
            Pageable pageable
    ) {
        // Repository에서 이미 모든 필드를 포함한 Response를 반환하므로 그대로 반환
        // 단, thumbnailUrl만 CDN URL로 변환
        var activities = activityRepository.findActivitiesByCondition(
                userId, category, tagIds, title, sortType, pageable
        );

        return activities.map(a -> new ActivityPreviewResponse(
                a.activityId(),
                a.title(),
                a.contextPreview(),
                thumbnailUrlOrNull(a.thumbnailUrl()),
                a.tags(),
                a.bookmarkCount(),
                a.organizer(),
                a.applyEndDate(),
                a.status(),
                a.createdAt()
        ));
    }

    @Transactional
    public ActivityPreviewResponse create(Long userId, ActivityRequest request) {
        Users userRef = userRepository.getReferenceById(userId);
        // 1. 엔티티 기본 저장
        ExternalActivity saved = activityRepository.save(ExternalActivity.builder()
                .user(userRef)
                .title(request.title())
                .category(request.category())
                .context(request.content())
                .thumbnailKey(DEFAULT_THUMB)
                .build());

        String finalAttachPrefix = "activity/activities/activity-" + saved.getActivityId() + "/attachments";
        String finalThumbPrefix = "activity/activities/activity-" + saved.getActivityId() + "/attachments/thumbnail";

        // 2. 썸네일 Consume
        if (StringUtils.hasText(request.thumbnailKey())) {
            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_THUMBNAIL,
                    UploadRefType.ACTIVITY,
                    saved.getActivityId(),
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            saved.updateThumbnailKey(finalKey);
        }

        // 3. 첨부파일 Consume 및 저장
        List<String> attachmentKeys = (request.attachmentKey() == null) ? List.of() : request.attachmentKey();

        for (String tempKey : attachmentKeys) {
            if (!StringUtils.hasText(tempKey)) continue;

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_ATTACHMENT,
                    UploadRefType.ACTIVITY,
                    saved.getActivityId(),
                    tempKey,
                    finalAttachPrefix
            );

            activityAttachmentRepository.save(ExternalActivityAttachment.builder()
                    .activity(saved)
                    .fileKey(finalKey)
                    .build());
        }

        // 4. 태그 저장
        saveTags(saved, request.tagIds());

        return new ActivityPreviewResponse(
                saved.getActivityId(),
                saved.getTitle(),
                saved.getContext(),
                thumbnailUrlOrNull(saved.getThumbnailKey()),
                null,
                0L, // 새로 생성된 활동이므로 북마크 수는 0
                saved.getOrganizer(),
                saved.getApplyEndDate(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public ActivityPreviewResponse createAdmin(Long userId, AdminActivityRequest request) {
        // 1. 관리자는 대외활동과 취업정보만 작성 가능
        if (request.category() != ActivityCategory.EXTERNAL && request.category() != ActivityCategory.RECRUITMENT) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        }

        Users adminUser = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 생성 및 저장
        ExternalActivity saved = activityRepository.save(ExternalActivity.builder()
                .user(adminUser)
                .title(request.title())
                .category(request.category())
                .organizer(request.organizer())
                .targetDescription(request.targetDescription())
                .applyStartDate(request.applyStartDate())
                .applyEndDate(request.applyEndDate())
                .resultAnnounceDate(request.resultAnnounceDate())
                .officialUrl(request.officialUrl())
                .contextTitle(request.contextTitle())
                .context(request.content())
                .thumbnailKey(DEFAULT_THUMB)
                .build());

        saveTags(saved, request.tagIds());

        // 3. 썸네일 처리 (관리자용은 userId를 0L 또는 특정 관리자 ID로 설정)
        String finalThumbPrefix = "activity/activities/activity-" + saved.getActivityId() + "/attachments/thumbnail";

        if (StringUtils.hasText(request.thumbnailKey())) {
            String finalKey = presignEngine.consume(
                    userId,  // 관리자용 userId (실제 관리자 ID로 변경 가능)
                    UploadPurpose.ACTIVITY_THUMBNAIL,
                    UploadRefType.ACTIVITY,
                    saved.getActivityId(),
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            saved.updateThumbnailKey(finalKey);
        }

        return new ActivityPreviewResponse(
                saved.getActivityId(),
                saved.getTitle(),
                saved.getContext(),
                thumbnailUrlOrNull(saved.getThumbnailKey()),
                null,
                0L,
                saved.getOrganizer(),
                saved.getApplyEndDate(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void update(Long userId, Long activityId, ActivityRequest request) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.getUser() == null || !Objects.equals(activity.getUser().getUserId(), userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        Set<String> deleteAfterCommit = new HashSet<>();
        String finalAttachPrefix = "activity/activities/activity-" + activity.getActivityId() + "/attachments";
        String finalThumbPrefix = "activity/activities/activity-" + activity.getActivityId() + "/attachments/thumbnail";

        // 1. 썸네일 교체 로직
        if (StringUtils.hasText(request.thumbnailKey())
                && !request.thumbnailKey().equals(activity.getThumbnailKey())) {

            // 기존 썸네일이 있으면 삭제 대상에 추가
            if (StringUtils.hasText(activity.getThumbnailKey()) && !DEFAULT_THUMB.equals(activity.getThumbnailKey())) {
                deleteAfterCommit.add(activity.getThumbnailKey());
            }

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_THUMBNAIL,
                    UploadRefType.ACTIVITY,
                    activityId,
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            activity.updateThumbnailKey(finalKey);
        }

        // 2. 첨부파일 교체 로직 (요청이 들어온 경우만)
        if (request.attachmentKey() != null) {
            // 기존 첨부파일 목록을 Map으로 관리
            Map<String, ExternalActivityAttachment> currentByKey =
                    activityAttachmentRepository.findAllByActivity_ActivityId(activityId).stream()
                            .filter(a -> StringUtils.hasText(a.getFileKey()))
                            .collect(Collectors.toMap(
                                    ExternalActivityAttachment::getFileKey,
                                    a -> a, (a, b) -> a
                            ));

            Set<String> keepKeys = new HashSet<>();
            LinkedHashSet<String> reqKeys = new LinkedHashSet<>();
            for (String k : request.attachmentKey()) if (StringUtils.hasText(k)) reqKeys.add(k);

            // 새로운 첨부파일 처리
            for (String k : reqKeys) {
                ExternalActivityAttachment existing = currentByKey.get(k);
                if (existing != null) {
                    keepKeys.add(k);
                    continue;
                } // 이미 존재하는 키면 유지

                // 새로운 temp 키면 consume
                String finalKey = presignEngine.consume(
                        userId,
                        UploadPurpose.ACTIVITY_ATTACHMENT,
                        UploadRefType.ACTIVITY,
                        activityId,
                        k,
                        finalAttachPrefix
                );

                activityAttachmentRepository.save(ExternalActivityAttachment.builder()
                        .activity(activity)
                        .fileKey(finalKey)
                        .build());

                keepKeys.add(finalKey);
            }

            // 삭제할 첨부파일 식별
            for (String oldKey : currentByKey.keySet()) {
                if (!keepKeys.contains(oldKey)) deleteAfterCommit.add(oldKey);
            }


            // DB에서 삭제할 첨부파일 제거
            activityAttachmentRepository.deleteAll(
                    currentByKey.values().stream()
                            .filter(a -> !keepKeys.contains(a.getFileKey()))
                            .collect(Collectors.toList())
            );
        }

        // 3. 기본 정보 업데이트
        activity.update(request);

        // 4. 태그 저장
        saveTags(activity, request.tagIds());

        // 5. S3 파일 삭제 예약
        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    @Transactional
    public void updateAdmin(Long activityId, AdminActivityRequest request) {
        // 1. 관리자는 대외활동과 취업정보만 수정 가능
        if (request.category() != ActivityCategory.EXTERNAL && request.category() != ActivityCategory.RECRUITMENT) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        }

        // 2. 활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 3. 관리자가 작성한 글인지 카테고리로 검증 (유저 검증 없이 카테고리로만 검증)
        if (activity.getCategory() != ActivityCategory.EXTERNAL && activity.getCategory() != ActivityCategory.RECRUITMENT) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        }

        Set<String> deleteAfterCommit = new HashSet<>();
        String finalThumbPrefix = "activity/activities/activity-" + activity.getActivityId() + "/attachments/thumbnail";

        // 4. 썸네일 교체 로직
        if (StringUtils.hasText(request.thumbnailKey())
                && !request.thumbnailKey().equals(activity.getThumbnailKey())) {

            // 기존 썸네일이 있으면 삭제 대상에 추가
            if (StringUtils.hasText(activity.getThumbnailKey()) && !DEFAULT_THUMB.equals(activity.getThumbnailKey())) {
                deleteAfterCommit.add(activity.getThumbnailKey());
            }

            // presignEngine.consume()에 userId 대신 activity 작성자 ID 사용
            Long authorId = activity.getUser().getUserId();
            String finalKey = presignEngine.consume(
                    authorId,
                    UploadPurpose.ACTIVITY_THUMBNAIL,
                    UploadRefType.ACTIVITY,
                    activityId,
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            activity.updateThumbnailKey(finalKey);
        }

        // 5. 기본 정보 업데이트 (ExternalActivity에 updateAdmin 메서드 추가 필요)
        activity.updateAdmin(request);

        // 6. S3 파일 삭제 예약
        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    @Transactional
    public void delete(Long activityId, Long userId) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.getUser() == null || !Objects.equals(activity.getUser().getUserId(), userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        Set<String> deleteAfterCommit = new HashSet<>();

        if (StringUtils.hasText(activity.getThumbnailKey()) && !DEFAULT_THUMB.equals(activity.getThumbnailKey())) {
            deleteAfterCommit.add(activity.getThumbnailKey());
        }

        activityAttachmentRepository.findAllByActivity_ActivityId(activityId)
                .forEach(a -> {
                    if (StringUtils.hasText(a.getFileKey())) deleteAfterCommit.add(a.getFileKey());
                });

        activityRepository.delete(activity);
        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    @Transactional(readOnly = true)
    public ActivityDetailResponse getActivityDetail(Long userId, Long activityId) {
        if (userId == null) throw new CustomException(ActivityErrorCode.USER_NOT_FOUND);
        // 1. 메인 활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 2. Activity → DTO 변환 + 썸네일 presign
        ExternalActivityDto activityDto = ExternalActivityDto.from(activity)
                .withThumbnailUrl(thumbnailUrlOrNull(activity.getThumbnailKey()));

        // 3. 첨부파일 조회 (카테고리 조건)
        List<ExternalActivityAttachmentDto> attachmentDtos = List.of();

        /// 글쓴이 프로필
        Long authorId = Optional.ofNullable(activity.getUser())
                .map(Users::getUserId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.USER_NOT_FOUND));

        AuthorDto author = authorAssembler.buildAuthorMap(List.of(authorId))
                .get(authorId);

        if (activity.getCategory() == ActivityCategory.CLUB || activity.getCategory() == ActivityCategory.STUDY) {

            List<ExternalActivityAttachment> atts =
                    activityAttachmentRepository.findAllByActivity_ActivityId(activityId);

            // fileKey 목록
            List<String> keys = atts.stream()
                    .map(ExternalActivityAttachment::getFileKey)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();

            // ticket bulk
            Map<String, UploadTicket> ticketMap = keys.isEmpty()
                    ? Map.of()
                    : uploadTicketRepository.findAllByStorageKeyIn(keys).stream()
                    .collect(Collectors.toMap(UploadTicket::getStorageKey, t -> t, (a, b) -> a));

            // presign (실패는 스킵)
            Map<String, String> urlMap = new HashMap<>();
            for (String key : keys) {
                UploadTicket t = ticketMap.get(key);
                String filename = (t == null) ? null : t.getOriginalFilename();
                String contentType = (t == null) ? null : t.getContentType();

                try {
                    String url = presignEngine.presignDownload(key, filename, contentType).downloadUrl();
                    if (StringUtils.hasText(url)) urlMap.put(key, url);
                } catch (Exception e) {
                    log.warn("activity presignDownload failed. activityId={}, key={}", activityId, key, e);
                }
            }

            attachmentDtos = atts.stream()
                    .filter(a -> StringUtils.hasText(a.getFileKey()))
                    .map(a -> {
                        String url = urlMap.get(a.getFileKey());
                        if (!StringUtils.hasText(url)) return null;
                        return ExternalActivityAttachmentDto.from(a).withFileUrl(url);
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
        // 4. 태그 리스트 조회
        List<Long> tagIds = activityTagRepository.findAllByActivity_ActivityId(activityId).stream()
                .map(t -> t.getTag().getId())
                .toList();

        // 5. 팀원 공고 리스트 조회
        List<TeamRecruitment> recruitmentList =
                teamRecruitmentRepository.findAllByActivityId(activityId);

        // 3. 스트림을 이용한 변환
        List<TeamRecruitmentDto> recruitmentDtoList = recruitmentList.stream()
                .map(recruitment -> {

                    String userName = userRepository.findNameByUserId(recruitment.getUserId()).orElse("알 수 없는 사용자");

                    return recruitment.toDto(activity.getTitle(), userName);
                })
                .toList();

        // 6. 본인 글 여부
        boolean isMine = activity.getUser() != null && Objects.equals(activity.getUser().getUserId(), userId);


        // 7. 북마크 수 조회
        Long bookmarkCount = activityBookmarkRepository.countByActivity_ActivityId(activityId);

        // 8. 북마크 여부 조회
        boolean isBookmarked = activityBookmarkRepository.existsByUser_UserIdAndActivity_ActivityId(userId, activityId);

        // 9. Response 생성
        return new ActivityDetailResponse(
                isMine,
                author,
                activityDto,
                attachmentDtos,
                tagIds,
                recruitmentDtoList,
                bookmarkCount,
                isBookmarked
        );
    }

    @Transactional
    public void closeActivity(Long userId, Long activityId) {
        // 1. 대외활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 2. 스터디와 동아리만 모집 마감 가능 (대외활동, 취업정보는 불가)
        if (activity.getCategory() != ActivityCategory.STUDY && activity.getCategory() != ActivityCategory.CLUB) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        }

        // 3. 작성자 본인 확인
        if (activity.getUser() == null || !Objects.equals(activity.getUser().getUserId(), userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        // 4. 이미 마감된 경우 예외 처리
        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new CustomException(ActivityErrorCode.ALREADY_CLOSED);
        }

        // 5. 상태를 CLOSED로 변경 (더티 체킹으로 자동 업데이트)
        activity.close();
    }

    @Transactional
    public void closeActivityAdmin(Long activityId) {
        // 1. 대외활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 2. 대외활동과 취업정보만 마감 가능 (관리자가 작성한 것만)
        if (activity.getCategory() != ActivityCategory.EXTERNAL && activity.getCategory() != ActivityCategory.RECRUITMENT) {
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        }

        // 4. 이미 마감된 경우 예외 처리
        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new CustomException(ActivityErrorCode.ALREADY_CLOSED);
        }

        // 5. 상태를 CLOSED로 변경
        activity.close();
    }

    @Transactional
    public boolean toggleActivityBookmark(Long userId, Long activityId) {
        // 대외활동 존재 여부 확인
        if (!activityRepository.existsById(activityId)) {
            throw new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        // 북마크 존재 여부 확인
        Optional<ExternalActivityBookmark> bookmarkOpt =
                activityBookmarkRepository.findByUser_UserIdAndActivity_ActivityId(userId, activityId);

        if (bookmarkOpt.isPresent()) {
            // 이미 존재하면 삭제
            activityBookmarkRepository.delete(bookmarkOpt.get());
            return false; // 해제됨을 반환
        } else {
            Users userRef = userRepository.getReferenceById(userId);
            ExternalActivity actRef = activityRepository.getReferenceById(activityId);

            ExternalActivityBookmark newBookmark = ExternalActivityBookmark.of(userRef, actRef);
            activityBookmarkRepository.save(newBookmark);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public HomeResponse.ContestSection getHomeContests(int limit) {

        List<Long> ids = activityRepository.findTopIdsByBookmark(ActivityCategory.EXTERNAL, limit + 1);
        if (ids.isEmpty()) return HomeResponse.ContestSection.empty();

        boolean hasMore = ids.size() > limit;
        List<Long> topIds = hasMore ? ids.subList(0, limit) : ids;

        Map<Long, ExternalActivity> map = activityRepository.findAllById(topIds).stream()
                .collect(Collectors.toMap(ExternalActivity::getActivityId, a -> a));

        List<HomeResponse.ContestSection.ContestCard> items = topIds.stream()
                .map(id -> {
                    ExternalActivity a = map.get(id);
                    if (a == null) return null;

                    String thumbUrl = thumbnailUrlOrNull(a.getThumbnailKey());
                    return new HomeResponse.ContestSection.ContestCard(
                            a.getActivityId(),
                            a.getTitle(),
                            a.getOrganizer(),
                            thumbUrl
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new HomeResponse.ContestSection(items, hasMore);
    }

    // --- Helper Methods ---

    /**
     * thumbnail 전용 URL 제공 메서드
     * CDN 방식
     */
    private String thumbnailUrlOrNull(String key) {
        if (!StringUtils.hasText(key) || DEFAULT_THUMB.equals(key)) return null;
        return publicUrlIssuer.issueImagePublicUrl(key);
    }

    /**
     * 활동의 태그 저장
     */
    private void saveTags(ExternalActivity activity, List<Long> tagIds) {
        if (tagIds != null) {
            activityTagRepository.deleteByActivity_ActivityId(activity.getActivityId());

            LinkedHashSet<Long> uniq = new LinkedHashSet<>(tagIds);
            for (Long id : uniq) {
                Tag tagRef = tagRepository.getReferenceById(id);
                activityTagRepository.save(
                        ExternalActivityTag.builder()
                                .activity(activity)
                                .tag(tagRef)
                                .build()
                );
            }
        }
    }
}