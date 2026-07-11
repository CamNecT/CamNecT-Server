package CamNecT.server.domain.community.controller;

import CamNecT.server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.server.domain.community.dto.request.UpdateCommentRequest;
import CamNecT.server.domain.community.dto.response.CreateCommentResponse;
import CamNecT.server.domain.community.dto.response.ToggleCommentLikeResponse;
import CamNecT.server.domain.community.service.CommentService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Community Comment", description = "커뮤니티 게시글 및 댓글 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "특정 게시글에 새로운 댓글 또는 대댓글을 작성합니다.")
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CreateCommentResponse> create(
            @UserId Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid CreateCommentRequest req
    ) {
        return ApiResponse.success(commentService.create(userId, postId, req));
    }

    // 댓글 목록 조회 (flat list: parentCommentId로 프론트에서 묶기)
    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글의 전체 댓글을 평면 리스트(Flat List) 형태로 조회합니다. 대댓글은 parentCommentId를 기준으로 프론트엔드에서 그룹화 처리가 필요합니다."
    )
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentService.CommentRow>> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(commentService.list(postId, size));
    }

    @Operation(summary = "댓글 수정", description = "작성한 댓글의 내용을 수정합니다.")
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<Void> update(
            @UserId Long userId,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest req
    ) {
        commentService.update(userId, commentId, req);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다. 답글이 있는 경우 로직에 따라 상태가 변경될 수 있습니다.")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> delete(
            @UserId Long userId,
            @PathVariable Long commentId
    ) {
        commentService.delete(userId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "댓글 좋아요 설정 (토글 방식)", description = "댓글의 좋아요 상태를 반전(Toggle)시킵니다. 호출 시마다 좋아요 등록/해제 상태와 총 좋아요 수를 반환합니다.")
    @PostMapping("/comments/{commentId}/likes")
    public ApiResponse<ToggleCommentLikeResponse> toggleCommentLike(
            @UserId Long userId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentService.toggleLike(userId, commentId));
    }


}
