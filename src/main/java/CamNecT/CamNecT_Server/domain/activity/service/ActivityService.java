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
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import CamNecT.CamNecT_Server.global.storage.service.FileStorage;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityService {

    private final ExternalActivityRepository activityRepository;
    private final ExternalActivityTagRepository activityTagRepository;
    private final ExternalActivityAttachmentRepository activityAttachmentRepository;
    private final ExternalActivityBookmarkRepository activityBookmarkRepository;
    private final TagRepository tagRepository;
    private final TeamRecruitmentRepository teamRecruitmentRepository;

    //S3 관련 의존성 주입
    private final PresignEngine presignEngine;
    private final UploadTicketRepository ticketRepo;
    private final FileStorage fileStorage;

    public Slice<ActivityPreviewResponse> getActivities(
            Long userId,
            ActivityCategory category,
            List<Long> tagIds,
            String title,
            String sortType,
            Pageable pageable) {

        Slice<ActivityPreviewResponse> activities = activityRepository.findActivitiesByCondition(
                userId, category, tagIds, title, sortType, pageable
        );

        // 조회 결과의 썸네일 URL을 presigned URL로 변환
        return activities.map(activity -> {
            String thumbnailKey = activity.thumbnailUrl(); // DB에 저장된 key
            String presignedUrl = presignOrNull(thumbnailKey, "thumbnail", null);

            return new ActivityPreviewResponse(
                    activity.activityId(),
                    activity.title(),
                    activity.context(),
                    presignedUrl,
                    activity.tags()
            );
        });
    }

    @Transactional
    public ActivityPreviewResponse create(Long userId, ActivityRequest request) {
        // 1. 엔티티 기본 저장
        ExternalActivity activity = ExternalActivity.builder()
                .userId(userId)
                .title(request.title())
                .category(request.category())
                .context(request.content())
                .thumbnailUrl("기본이미지")
                .build();
        ExternalActivity saved = activityRepository.save(activity);

        String finalThumbPrefix = "activity/user-" + userId + "/activity-" + saved.getActivityId() + "/thumbnail";
        String finalAttachPrefix = "activity/user-" + userId + "/activity-" + saved.getActivityId() + "/attachments";

        // 2. 썸네일 Consume
        if (StringUtils.hasText(request.thumbnailKey())) {
            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_ATTACHMENT,
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

            UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                    .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

            activityAttachmentRepository.save(ExternalActivityAttachment.builder()
                    .externalActivity(saved.getActivityId())
                    .fileUrl(finalKey)
                    .build());
        }

        // 4. 태그 저장
        saveTags(saved.getActivityId(), request.tagIds());

        return new ActivityPreviewResponse(
                saved.getActivityId(),
                saved.getTitle(),
                saved.getContext(),
                presignOrNull(saved.getThumbnailUrl(), "thumbnail", null),
                null
        );
    }

    @Transactional
    public void update(Long userId, Long activityId, ActivityRequest request) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        Set<String> deleteAfterCommit = new HashSet<>();
        String finalThumbPrefix = "activity/user-" + userId + "/activity-" + activityId + "/thumbnail";
        String finalAttachPrefix = "activity/user-" + userId + "/activity-" + activityId + "/attachments";

        // 1. 썸네일 교체 로직
        if (StringUtils.hasText(request.thumbnailKey())
                && !request.thumbnailKey().equals(activity.getThumbnailUrl())) {

            // 기존 썸네일이 있으면 삭제 대상에 추가
            if (StringUtils.hasText(activity.getThumbnailUrl()) && !"기본이미지".equals(activity.getThumbnailUrl())) {
                deleteAfterCommit.add(activity.getThumbnailUrl());
            }

            String finalKey = presignEngine.consume(
                    userId,
                    UploadPurpose.ACTIVITY_ATTACHMENT,
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
                            .filter(a -> StringUtils.hasText(a.getFileUrl()))
                            .collect(Collectors.toMap(ExternalActivityAttachment::getFileUrl, a -> a));

            Set<String> keepKeys = new HashSet<>();

            // 요청된 키 목록 (중복 제거)
            LinkedHashSet<String> reqKeys = new LinkedHashSet<>();
            for (String k : request.attachmentKey()) {
                if (StringUtils.hasText(k)) reqKeys.add(k);
            }

            // 새로운 첨부파일 처리
            for (String k : reqKeys) {
                ExternalActivityAttachment existing = currentByKey.get(k);

                // 이미 존재하는 키면 유지
                if (existing != null) {
                    keepKeys.add(k);
                    continue;
                }

                // 새로운 temp 키면 consume
                String finalKey = presignEngine.consume(
                        userId,
                        UploadPurpose.ACTIVITY_ATTACHMENT,
                        UploadRefType.ACTIVITY,
                        activityId,
                        k,
                        finalAttachPrefix
                );

                UploadTicket t = ticketRepo.findByStorageKey(finalKey)
                        .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

                activityAttachmentRepository.save(ExternalActivityAttachment.builder()
                        .externalActivity(activityId)
                        .fileUrl(finalKey)
                        .build());

                keepKeys.add(finalKey);
            }

            // 삭제할 첨부파일 식별
            for (String oldKey : currentByKey.keySet()) {
                if (!keepKeys.contains(oldKey)) {
                    deleteAfterCommit.add(oldKey);
                }
            }

            // DB에서 삭제할 첨부파일 제거
            activityAttachmentRepository.deleteAll(
                    currentByKey.values().stream()
                            .filter(a -> !keepKeys.contains(a.getFileUrl()))
                            .collect(Collectors.toList())
            );
        }

        // 3. 기본 정보 업데이트
        activity.update(request);

        // 4. 태그 저장
        saveTags(activityId, request.tagIds());

        // 5. S3 파일 삭제 예약
        registerAfterCommitDelete(deleteAfterCommit);
    }

    @Transactional
    public void delete(Long activityId, Long userId) {
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (!activity.getUserId().equals(userId)) {
            throw new CustomException(ActivityErrorCode.NOT_AUTHOR);
        }

        Set<String> deleteAfterCommit = new HashSet<>();

        // 썸네일 삭제 대상 추가
        if (StringUtils.hasText(activity.getThumbnailUrl()) && !"기본이미지".equals(activity.getThumbnailUrl())) {
            deleteAfterCommit.add(activity.getThumbnailUrl());
        }

        // 첨부파일 삭제 대상 추가
        activityAttachmentRepository.findAllByExternalActivity(activityId)
                .forEach(a -> {
                    if (StringUtils.hasText(a.getFileUrl())) {
                        deleteAfterCommit.add(a.getFileUrl());
                    }
                });

        activityRepository.delete(activity);
        registerAfterCommitDelete(deleteAfterCommit);
    }

    @Transactional(readOnly = true)
    public ActivityDetailResponse getActivityDetail(Long userId, Long activityId) {

        // 1. 메인 활동 조회
        ExternalActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        // 2. Activity → DTO 변환 + 썸네일 presign
        ExternalActivityDto activityDto =
                ExternalActivityDto.from(activity)
                        .withThumbnailUrl(
                                presignOrNull(activity.getThumbnailUrl(), "thumbnail", null)
                        );

        // 3. 첨부파일 조회 (카테고리 조건)
        List<ExternalActivityAttachmentDto> attachmentDtos = null;

        if (activity.getCategory() == ActivityCategory.EXTERNAL
                || activity.getCategory() == ActivityCategory.RECRUITMENT) {

            attachmentDtos = activityAttachmentRepository.findAllByExternalActivity(activityId)
                    .stream()
                    .map(a -> ExternalActivityAttachmentDto.from(a)
                            .withFileUrl(
                                    presignOrNull(a.getFileUrl(), "attachment", null)
                            ))
                    .toList();
        }

        // 4. 태그 리스트 조회
        List<Long> tagIds = activityTagRepository.findAllByActivityId(activityId).stream()
                .map(ExternalActivityTag::getTagId)
                .toList();
        List<Tag> tagList = tagRepository.findAllById(tagIds);

        // 5. 팀원 공고 리스트 조회
        List<TeamRecruitment> recruitmentList =
                teamRecruitmentRepository.findAllByActivityId(activityId);

        // 6. 본인 글 여부
        boolean isMine = activity.getUserId() == null || activity.getUserId().equals(userId);

        // 7. Response 생성
        return new ActivityDetailResponse(
                isMine,
                activityDto,
                attachmentDtos,
                tagList,
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

    // --- Helper Methods ---

    /**
     * S3 key를 presigned download URL로 변환
     * Portfolio 방식과 동일한 로직
     */
    private String presignOrNull(String key, String filename, String contentType) {
        if (!StringUtils.hasText(key) || "기본이미지".equals(key)) return null;
        try {
            return presignEngine.presignDownload(key, filename, contentType).downloadUrl();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 트랜잭션 커밋 후 S3 파일 삭제
     * Portfolio 방식과 동일한 로직 (트랜잭션 활성 상태 체크 추가)
     */
    private void registerAfterCommitDelete(Set<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String key : keys) {
                        try {
                            fileStorage.delete(key);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            // 트랜잭션이 없는 경우 즉시 삭제
            for (String key : keys) {
                try {
                    fileStorage.delete(key);
                } catch (Exception ignored) {
                }
            }
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