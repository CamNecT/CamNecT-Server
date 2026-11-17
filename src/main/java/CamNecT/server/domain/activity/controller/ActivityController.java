package CamNecT.server.domain.activity.controller;

import org.springdoc.core.annotations.ParameterObject;
import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import CamNecT.server.domain.activity.dto.request.ActivityRequest;
import CamNecT.server.domain.activity.dto.request.AdminActivityRequest;
import CamNecT.server.domain.activity.dto.response.ActivityDetailResponse;
import CamNecT.server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.service.ActivityAttachmentService;
import CamNecT.server.domain.activity.service.ActivityService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.server.global.storage.dto.response.PresignUploadBatchResponse;
import CamNecT.server.global.storage.dto.response.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final ActivityAttachmentService activityAttachmentService;

    @Operation(
            summary = "대외활동 목록 조회",
            description = "카테고리(enum타입 - STUDY, CLUB, EXTERNAL, RECRUITMENT), 태그, 제목, 정렬 기준(enum타입 - RECOMMEND, DEADLINE, BOOKMARK, RECRUIT, LATEST), 을 적용하여 활동 목록을 무한 스크롤(Slice) 방식으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 카테고리·태그·페이지 요청 형식 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ApiResponse<Slice<ActivityPreviewResponse>> getActivities(
            @UserId Long userId,
            @RequestParam(required = false) ActivityCategory category,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "LATEST") String sortType,
            @ParameterObject Pageable pageable) {

        return ApiResponse.success(activityService.getActivities(userId, category, tagIds, title, sortType, pageable));
    }

    @Operation(summary = "대외활동 등록", description = "동아리/스터디에 해당하는 대외활동을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49010 만료·사용된 업로드 티켓 / 49011 업로드 파일 불일치 / 46004 지원하지 않는 활동 카테고리 / 46007 유효하지 않은 태그", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "49310 업로드 티켓 사용 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "49410 업로드 티켓 / 49401 저장 파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "49902 업로드 파일 확인 실패 / 49904 파일 이동 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ApiResponse<ActivityPreviewResponse> create(
            @UserId Long userId,
            @RequestBody @Valid ActivityRequest request
    ) {
        return ApiResponse.success(activityService.create(userId, request));
    }

    @Operation(summary = "대외활동 수정", description = "기존 대외활동 게시글을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49010 만료·사용된 업로드 티켓 / 49011 업로드 파일 불일치 / 46004 지원하지 않는 활동 카테고리 / 46007 유효하지 않은 태그", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "46101 활동 작성자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "49310 업로드 티켓 사용 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "49410 업로드 티켓 / 49401 저장 파일을 찾을 수 없음 / 46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "49902 업로드 파일 확인 실패 / 49904 파일 이동 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 경로·쿼리 자료형 또는 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46401 활동 작성자 / 46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> getActivityDetail(
            @UserId Long userId,
            @PathVariable Long activityId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(activityService.getActivityDetail(userId, activityId, "GET".equals(request.getMethod())));
    }

    @Operation(summary = "대외활동 삭제", description = "특정 대외활동을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 경로·쿼리 자료형 또는 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "46101 활동 작성자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "46006 팀원 모집글이 있는 활동은 삭제할 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{activityId}")
    public ApiResponse<Long> delete(
            @PathVariable Long activityId,
            @UserId Long userId
    ) {
        activityService.delete(activityId, userId);
        return ApiResponse.success(activityId);
    }

    @Operation(summary = "대외활동 모집 마감", description = "대외활동 모집을 마감합니다. 스터디/동아리만 마감할 수 있으며, 작성자만 마감할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "46004 지원하지 않는 활동 카테고리 / 46005 이미 마감된 활동", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "46101 활동 작성자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{activityId}/close")
    public ApiResponse<String> closeActivity(
            @PathVariable Long activityId,
            @UserId Long userId
    ){
        activityService.closeActivity(userId, activityId);
        return ApiResponse.success("대외활동이 성공적으로 모집 완료되었습니다.");
    }

    @Operation(summary = "대외활동 북마크 설정 (토글 방식)", description = "활동의 북마크 상태를 반전(Toggle)시킵니다. 등록 시 등록 메시지, 해제 시 해제 메시지를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 경로·쿼리 자료형 또는 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    // --- 관리자 API ---
    @Operation(
            summary = "[관리자] 대외활동/취업정보 등록",
            description = "관리자가 대외활동 또는 취업정보 게시글을 작성합니다. EXTERNAL(대외활동) 또는 RECRUITMENT(취업정보) 카테고리만 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49010 만료·사용된 업로드 티켓 / 49011 업로드 파일 불일치 / 46004 지원하지 않는 활동 카테고리 / 46007 유효하지 않은 태그", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음 / 44303 관리자 사용자가 아님 / 49310 업로드 티켓 사용 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "49410 업로드 티켓 / 49401 저장 파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "49902 업로드 파일 확인 실패 / 49904 파일 이동 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/admin")
    public ApiResponse<ActivityPreviewResponse> createAdmin(
            @UserId Long adminId,
            @RequestBody @Valid AdminActivityRequest request
    ){
        return ApiResponse.success(activityService.createAdmin(adminId, request));
    }

    @Operation(
            summary = "[관리자] 대외활동/취업정보 수정",
            description = "관리자가 작성한 대외활동 또는 취업정보 게시글을 수정합니다. EXTERNAL(대외활동) 또는 RECRUITMENT(취업정보) 카테고리만 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49010 만료·사용된 업로드 티켓 / 49011 업로드 파일 불일치 / 46004 지원하지 않는 활동 카테고리 / 46007 유효하지 않은 태그", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음 / 44303 관리자 사용자가 아님 / 49310 업로드 티켓 사용 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "49410 업로드 티켓 / 49401 저장 파일을 찾을 수 없음 / 46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "49902 업로드 파일 확인 실패 / 49904 파일 이동 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/admin/{activityId}")
    public ApiResponse<String> updateAdmin(
            @UserId Long adminId,
            @PathVariable Long activityId,
            @RequestBody @Valid AdminActivityRequest request
    ) {
        activityService.updateAdmin(adminId, activityId, request);
        return ApiResponse.success("수정이 완료되었습니다.");
    }

    @Operation(
            summary = "[관리자] 대외활동/취업정보 모집 마감",
            description = "관리자가 작성한 대외활동 또는 취업정보 게시글을 마감합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "46004 지원하지 않는 활동 카테고리 / 46005 이미 마감된 활동", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음 / 44303 관리자 사용자가 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/admin/{activityId}/close")
    public ApiResponse<String> closeActivityAdmin(
            @UserId Long adminId,
            @PathVariable Long activityId
    ){
        activityService.closeActivityAdmin(adminId, activityId);
        return ApiResponse.success("대외활동이 성공적으로 모집 완료되었습니다.");
    }


    // --- S3 Presign URL 발급 API 추가 ---
    @Operation(summary = "대외활동 썸네일 업로드용 Presigned URL 발급")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49020 파일 크기가 0 이하", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "49005 파일 용량 제한 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type / 49004 허용하지 않는 파일 형식", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "49006 파일 개수 또는 미사용 업로드 티켓 제한 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/uploads/presign/thumbnail")
    public ApiResponse<PresignUploadResponse> presignThumbnail(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        return ApiResponse.success(activityAttachmentService.presignThumbnail(userId, req));
    }

    @Operation(summary = "대외활동 첨부 업로드용 Presigned URL 발급 (Batch)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 49020 파일 크기가 0 이하", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "49005 파일 용량 제한 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type / 49004 허용하지 않는 파일 형식", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "49006 파일 개수 또는 미사용 업로드 티켓 제한 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/uploads/presign/attachments")
    public ApiResponse<PresignUploadBatchResponse> presignAttachments(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadBatchRequest req
    ) {
        return ApiResponse.success(activityAttachmentService.presignAttachmentsBatch(userId, req));
    }
}
