package CamNecT.CamNecT_Server.domain.activity.controller;

import CamNecT.CamNecT_Server.domain.activity.dto.request.ActivityRequest;
import CamNecT.CamNecT_Server.domain.activity.dto.response.ActivityDetailResponse;
import CamNecT.CamNecT_Server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.CamNecT_Server.domain.activity.service.ActivityService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Activity", description = "대외활동(동아리/스터디/대외활동/취업정보) 관련 API")
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final PresignEngine presignEngine;

    @Operation(
            summary = "대외활동 목록 조회",
            description = "카테고리(enum타입 - STUDY, CLUB, EXTERNAL, RECRUITMENT), 태그, 제목, 정렬 기준(enum타입 - RECOMMEND, DEADLINE, BOOKMARK, RECRUIT, LATEST), 을 적용하여 활동 목록을 무한 스크롤(Slice) 방식으로 조회합니다."
    )
    @GetMapping
    public ApiResponse<Slice<ActivityPreviewResponse>> getActivities(
            @UserId Long userId,
            @RequestParam(required = false) ActivityCategory category,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "LATEST") String sortType,
            Pageable pageable) {

        return ApiResponse.success(activityService.getActivities(userId, category, tagIds, title, sortType, pageable));
    }

    @Operation(summary = "대외활동 등록", description = "동아리/스터디에 해당하는 대외활동을 생성합니다.")
    @PostMapping
    public ApiResponse<ActivityPreviewResponse> create(
            @UserId Long userId,
            @RequestBody @Valid ActivityRequest request
    ) {
        return ApiResponse.success(activityService.create(userId, request));
    }

    @Operation(summary = "대외활동 수정", description = "기존 대외활동 게시글을 수정합니다.")
    @PatchMapping("/{activityId}")
    public ApiResponse<String> update(
            @UserId Long userId,
            @PathVariable Long activityId,
            @RequestBody @Valid ActivityRequest request
    ) {
        activityService.update(userId, activityId, request);

        return ApiResponse.success("수정이 완료되었습니다.");
    }

    @Operation(summary = "대외활동 상세 조회", description = "특정 대외활동의 상세 정보를 조회합니다.")
    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> getActivityDetail(
            @UserId Long userId,
            @PathVariable Long activityId
    ) {
        return ApiResponse.success(activityService.getActivityDetail(userId, activityId));
    }

    @Operation(summary = "대외활동 삭제", description = "특정 대외활동을 삭제합니다.")
    @DeleteMapping("/{activityId}")
    public ApiResponse<Long> delete(
            @PathVariable Long activityId,
            @UserId Long userId
    ) {
        activityService.delete(activityId, userId);
        return ApiResponse.success(activityId);
    }

    @Operation(summary = "대외활동 북마크 설정 (토글 방식)", description = "활동의 북마크 상태를 반전(Toggle)시킵니다. 등록 시 등록 메시지, 해제 시 해제 메시지를 반환합니다.")
    @PostMapping("/{activityId}/bookmark")
    public ApiResponse<String> toggleBookmark(
            @UserId Long userId,
            @PathVariable Long activityId
    ) {
        // true: 북마크 등록됨, false: 북마크 해제됨
        boolean isBookmarked = activityService.toggleActivityBookmark(userId, activityId);
        String message = isBookmarked ? "북마크가 등록되었습니다." : "북마크가 해제되었습니다.";

        return ApiResponse.success(message);
    }

    // --- S3 Presign URL 발급 API 추가 ---

    @Operation(summary = "대외활동 썸네일 업로드용 Presigned URL 발급", description = "대외활동 썸네일 이미지를 S3에 업로드하기 위한 사전 승인 URL을 발급받습니다.")
    @PostMapping("/uploads/presign/thumbnail")
    public ApiResponse<PresignUploadResponse> presignThumbnail(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        String keyPrefix = "activity/user-" + userId + "/thumbnail";
        return ApiResponse.success(presignEngine.issueUpload(userId, UploadPurpose.ACTIVITY_ATTACHMENT, keyPrefix, req.contentType(), req.size(), req.originalFilename()));
    }

    @Operation(summary = "대외활동 첨부파일 업로드용 Presigned URL 발급", description = "대외활동 관련 첨부파일을 S3에 업로드하기 위한 사전 승인 URL을 발급받습니다.")
    @PostMapping("/uploads/presign/attachment")
    public ApiResponse<PresignUploadResponse> presignAttachment(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        String keyPrefix = "activity/user-" + userId + "/attachments";
        return ApiResponse.success(presignEngine.issueUpload(userId, UploadPurpose.ACTIVITY_ATTACHMENT, keyPrefix, req.contentType(), req.size(), req.originalFilename()));
    }

}