package CamNecT.server.domain.activity.controller;

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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    @PostMapping("/{activityId}/comments")
    public ApiResponse<CreateCommentResponse> create(@UserId Long userId, @PathVariable @Positive Long activityId,
                                                      @RequestBody @Valid CreateCommentRequest request) {
        return ApiResponse.success(service.create(userId, activityId, request));
    }

    @Operation(summary = "댓글 목록 조회", description = "루트 댓글 기준 커서 페이지와 대댓글을 평면 목록으로 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    @GetMapping("/{activityId}/comments")
    public ApiResponse<CommentListResponse> list(@UserId Long userId, @PathVariable @Positive Long activityId,
            @RequestParam(required = false) @Positive Long cursorId, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(service.list(userId, activityId, cursorId, size));
    }

    @Operation(summary = "댓글 수정")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<Void> update(@UserId Long userId, @PathVariable @Positive Long commentId,
                                    @RequestBody @Valid UpdateCommentRequest request) {
        service.update(userId, commentId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 삭제")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> delete(@UserId Long userId, @PathVariable @Positive Long commentId) {
        service.delete(userId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 좋아요 토글")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    @PostMapping("/comments/{commentId}/likes")
    public ApiResponse<ToggleCommentLikeResponse> toggleLike(@UserId Long userId, @PathVariable @Positive Long commentId) {
        return ApiResponse.success(service.toggleLike(userId, commentId));
    }
}
