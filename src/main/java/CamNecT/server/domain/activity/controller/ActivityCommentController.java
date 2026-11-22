package CamNecT.server.domain.activity.controller;

import CamNecT.server.global.common.auth.GuestReadable;
import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import CamNecT.server.domain.activity.dto.response.ActivityCommentListResponse;
import CamNecT.server.domain.activity.service.ActivityCommentService;
import CamNecT.server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.server.domain.community.dto.request.UpdateCommentRequest;
import CamNecT.server.domain.community.dto.response.CommentListResponse;
import CamNecT.server.domain.community.dto.response.CreateCommentResponse;
import CamNecT.server.domain.community.dto.response.ToggleCommentLikeResponse;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity Comment", description = "동아리 및 스터디 댓글 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/activity")
public class ActivityCommentController {
    private final ActivityCommentService service;

    @Operation(summary = "댓글 및 대댓글 작성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청 필드 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리 / 43030 부모 댓글이 다른 활동에 속함", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동 / 43412 부모 댓글을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "43910 숨김 부모 댓글 / 43911 대댓글 깊이 제한 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{activityId}/comments")
    public ApiResponse<CreateCommentResponse> create(@UserId Long userId, @PathVariable @Positive Long activityId,
                                                      @RequestBody @Valid CreateCommentRequest request) {
        return ApiResponse.success(service.create(userId, activityId, request));
    }

    @Operation(summary = "댓글 목록 조회", description = "루트 댓글 기준 커서 페이지와 루트별 최대 20개 대댓글을 평면 목록으로 반환합니다. replyPages의 nextCursorId로 대댓글을 이어서 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 ID·커서·size 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리 / 43040 유효하지 않은 커서", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GuestReadable
    @GetMapping("/{activityId}/comments")
    public ApiResponse<ActivityCommentListResponse> list(@UserId Long userId, @PathVariable @Positive Long activityId,
            @RequestParam(required = false) @Positive Long cursorId, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(service.list(userId, activityId, cursorId, size));
    }

    @Operation(summary = "대댓글 이어보기", description = "루트 댓글의 대댓글을 작성 시각과 ID 오름차순으로 반환합니다. cursorId를 생략하면 처음부터 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 ID·커서·size 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리 / 43040 유효하지 않은 커서", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동 / 43411 루트 댓글을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GuestReadable
    @GetMapping("/{activityId}/comments/{commentId}/replies")
    public ApiResponse<CommentListResponse> replies(@UserId Long userId, @PathVariable @Positive Long activityId,
            @PathVariable @Positive Long commentId, @RequestParam(required = false) @Positive Long cursorId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(service.listReplies(userId, activityId, commentId, cursorId, size));
    }

    @Operation(summary = "댓글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 ID 또는 요청 필드 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "43310 댓글 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동 / 43411 댓글을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "43912 공개 상태가 아닌 댓글", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<Void> update(@UserId Long userId, @PathVariable @Positive Long commentId,
                                    @RequestBody @Valid UpdateCommentRequest request) {
        service.update(userId, commentId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 ID 또는 요청 필드 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "43310 댓글 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동 / 43411 댓글을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "43912 공개 상태가 아닌 댓글", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> delete(@UserId Long userId, @PathVariable @Positive Long commentId) {
        service.delete(userId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 좋아요 토글")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 ID 또는 요청 필드 검증 실패 / 46004 댓글을 지원하지 않는 활동 카테고리", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "46402 활동 / 43411 댓글을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "43912 공개 상태가 아닌 댓글", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/comments/{commentId}/likes")
    public ApiResponse<ToggleCommentLikeResponse> toggleLike(@UserId Long userId, @PathVariable @Positive Long commentId) {
        return ApiResponse.success(service.toggleLike(userId, commentId));
    }
}
