package CamNecT.CamNecT_Server.domain.activity.service;

import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityDto;
import CamNecT.CamNecT_Server.domain.activity.dto.request.ActivityRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.response.ActivityDetailResponse;
import CamNecT.CamNecT_Server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityAttachment;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityBookmark;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityTag;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityAttachmentRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityBookmarkRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityTagRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.CamNecT_Server.domain.home.dto.HomeResponse;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.CamNecT_Server.global.common.service.GlobalPresignMethods;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
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
        var activities = activityRepository.findActivitiesByCondition(
                userId, category, tagIds, title, sortType, pageable
        );

        return activities.map(a -> new ActivityPreviewResponse(
                a.activityId(),
                a.title(),
                a.context(),
                thumbnailUrlOrNull(a.thumbnailUrl()),
                a.tags()
        ));
    }

    @Transactional
    public ActivityPreviewResponse create(Long userId, ActivityRequest request) {
        // 1. 엔티티 기본 저장
        ExternalActivity saved = activityRepository.save(ExternalActivity.builder()
                .userId(userId)
                .title(request.title())
                .category(request.category())
                .context(request.content())
                .thumbnailUrl(DEFAULT_THUMB)
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
            saved.updateThumbnail(finalKey);
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
                    .externalActivity(saved.getActivityId())
                    .fileKey(finalKey)
                    .build());
        }

        // 4. 태그 저장
        saveTags(saved.getActivityId(), request.tagIds());

        return new ActivityPreviewResponse(
                saved.getActivityId(),
                saved.getTitle(),
                saved.getContext(),
                thumbnailUrlOrNull(saved.getThumbnailUrl()),
                null
        );
    }

    @Transactional
    public void update(Long userId, Long activityId, ActivityRequest request) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (!Objects.equals(activity.getUserId(), userId)) throw new CustomException(ActivityErrorCode.NOT_AUTHOR);

        Set<String> deleteAfterCommit = new HashSet<>();
        String finalAttachPrefix = "activity/activities/activity-" + activity.getActivityId() + "/attachments";
        String finalThumbPrefix = "activity/activities/activity-" + activity.getActivityId() + "/attachments/thumbnail";

        // 1. 썸네일 교체 로직
        if (StringUtils.hasText(request.thumbnailKey())
                && !request.thumbnailKey().equals(activity.getThumbnailUrl())) {

            // 기존 썸네일이 있으면 삭제 대상에 추가
            if (StringUtils.hasText(activity.getThumbnailUrl()) && !DEFAULT_THUMB.equals(activity.getThumbnailUrl())) {
                deleteAfterCommit.add(activity.getThumbnailUrl());
            }

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_THUMBNAIL,
                    UploadRefType.ACTIVITY,
                    activityId,
                    request.thumbnailKey(),
                    finalThumbPrefix
            );
            activity.updateThumbnail(finalKey);
        }

        // 2. 첨부파일 교체 로직 (요청이 들어온 경우만)
        if (request.attachmentKey() != null) {
            // 기존 첨부파일 목록을 Map으로 관리
            Map<String, ExternalActivityAttachment> currentByKey =
                    activityAttachmentRepository.findAllByExternalActivity(activityId).stream()
                            .filter(a -> StringUtils.hasText(a.getFileKey()))
                            .collect(Collectors.toMap(ExternalActivityAttachment::getFileKey, a -> a));

            Set<String> keepKeys = new HashSet<>();
            LinkedHashSet<String> reqKeys = new LinkedHashSet<>();
            for (String k : request.attachmentKey()) if (StringUtils.hasText(k)) reqKeys.add(k);

            // 새로운 첨부파일 처리
            for (String k : reqKeys) {
                ExternalActivityAttachment existing = currentByKey.get(k);
                if (existing != null) { keepKeys.add(k); continue; } // 이미 존재하는 키면 유지

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
                        .externalActivity(activityId)
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
        saveTags(activityId, request.tagIds());

        // 5. S3 파일 삭제 예약
        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    @Transactional
    public void delete(Long activityId, Long userId) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (!Objects.equals(activity.getUserId(), userId)) throw new CustomException(ActivityErrorCode.NOT_AUTHOR);


        Set<String> deleteAfterCommit = new HashSet<>();

        if (StringUtils.hasText(activity.getThumbnailUrl()) && !DEFAULT_THUMB.equals(activity.getThumbnailUrl())) {
            deleteAfterCommit.add(activity.getThumbnailUrl());
        }

        activityAttachmentRepository.findAllByExternalActivity(activityId)
                .forEach(a -> { if (StringUtils.hasText(a.getFileKey())) deleteAfterCommit.add(a.getFileKey()); });

        activityRepository.delete(activity);
        globalPresignMethods.deleteAfterCommit(deleteAfterCommit);
    }

    @Transactional(readOnly = true)
    public ActivityDetailResponse getActivityDetail(Long userId, Long activityId) {
        // 1. 메인 활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 2. Activity → DTO 변환 + 썸네일 presign
        ExternalActivityDto activityDto = ExternalActivityDto.from(activity)
                .withThumbnailUrl(thumbnailUrlOrNull(activity.getThumbnailUrl()));

        // 3. 첨부파일 조회 (카테고리 조건)
        List<ExternalActivityAttachmentDto> attachmentDtos = null;

        if (activity.getCategory() == ActivityCategory.EXTERNAL || activity.getCategory() == ActivityCategory.RECRUITMENT) {

            List<ExternalActivityAttachment> atts =
                    activityAttachmentRepository.findAllByExternalActivity(activityId);

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
                    .collect(Collectors.toMap(UploadTicket::getStorageKey, t -> t, (a,b) -> a));

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
        List<Long> tagIds = activityTagRepository.findAllByActivityId(activityId).stream()
                .map(ExternalActivityTag::getTagId)
                .toList();
        List<String> tagNames = tagRepository.findNamesByIds(tagIds);

        // 5. 팀원 공고 리스트 조회
        List<TeamRecruitment> recruitmentList =
                teamRecruitmentRepository.findAllByActivityId(activityId);

        // 6. 본인 글 여부
        boolean isMine = Objects.equals(activity.getUserId(), userId);

        // 7. Response 생성
        return new ActivityDetailResponse(
                isMine,
                activityDto,
                attachmentDtos,
                tagNames,
                recruitmentList
        );
    }

    @Transactional
    public boolean toggleActivityBookmark(Long userId, Long activityId) {
        // 대외활동 존재 여부 확인
        if (!activityRepository.existsById(activityId)) {
            throw new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        // 북마크 존재 여부 확인
        Optional<ExternalActivityBookmark> bookmarkOpt =
                activityBookmarkRepository.findByUserIdAndActivityId(userId, activityId);

        if (bookmarkOpt.isPresent()) {
            // 이미 존재하면 삭제
            activityBookmarkRepository.delete(bookmarkOpt.get());
            return false; // 해제됨을 반환
        } else {
            // 존재하지 않으면 생성
            ExternalActivityBookmark newBookmark = ExternalActivityBookmark.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .build();
            activityBookmarkRepository.save(newBookmark);
            return true; // 등록됨을 반환
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

                    String thumbUrl = thumbnailUrlOrNull(a.getThumbnailUrl());
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
        try {
            return publicUrlIssuer.issuePublicUrl(key);
        } catch (Exception e) {
            log.warn("issuePublicUrl failed. key={}", key, e);
            return null;
        }
    }
    /**
     * 활동의 태그 저장
     */
    private void saveTags(Long activityId, List<Long> tagIds) {
        if (tagIds != null) {
            activityTagRepository.deleteByActivityId(activityId);
            tagIds.forEach(id -> activityTagRepository.save(
                    ExternalActivityTag.builder()
                            .activityId(activityId)
                            .tagId(id)
                            .build()
            ));
        }
    }
}