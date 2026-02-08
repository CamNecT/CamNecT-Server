package CamNecT.CamNecT_Server.domain.community.controller;

import CamNecT.CamNecT_Server.domain.community.dto.request.CreatePostRequest;
import CamNecT.CamNecT_Server.domain.community.dto.request.UpdatePostRequest;
import CamNecT.CamNecT_Server.domain.community.dto.response.*;
import CamNecT.CamNecT_Server.domain.community.service.PostAttachmentDownloadService;
import CamNecT.CamNecT_Server.domain.community.service.PostAttachmentsService;
import CamNecT.CamNecT_Server.domain.community.service.PostQueryService;
import CamNecT.CamNecT_Server.domain.community.service.PostQueryService.*;
import CamNecT.CamNecT_Server.domain.community.service.PostService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadBatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Community Post", description = "커뮤니티 게시글 관련 API (CRUD, 좋아요, 북마크, 채택, 구매)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class PostController {

    private final PostService postService;
    private final PostQueryService postQueryService;
    private final PostAttachmentDownloadService postAttachmentDownloadService;
    private final PostAttachmentsService postAttachmentsService;

    @Operation(summary = "게시글 작성", description = "새로운 게시글을 등록합니다. 첨부파일이 있는 경우 Presigned URL 발급 API를 먼저 호출해야 합니다.")
    @PostMapping
    public ApiResponse<CreatePostResponse> create(
            @UserId Long userId,
            @RequestBody @Valid CreatePostRequest req
    ) {
        return ApiResponse.success(postService.create(userId, req));
    }

    //리스트: /api/community/posts
    @Operation(
            summary = "게시글 목록 조회 (No-Offset 페이징)",
            description = "탭(Tab), 정렬(Sort), 태그, 키워드 검색을 지원하는 게시글 목록 조회 API입니다. 커서 기반 페이징(cursorId, cursorValue)을 사용합니다."
    )
    @GetMapping
    public ApiResponse<PostListResponse> list(
            @RequestParam(defaultValue = "ALL") Tab tab,
            @RequestParam(defaultValue = "RECOMMENDED") Sort sort,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long cursorValue,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(postQueryService.getPosts(tab, sort, tagId, keyword, cursorId, cursorValue, size));
    }

    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글의 내용을 수정합니다.")
    @PatchMapping("/{postId}")
    public ApiResponse<Void> update(
            @UserId Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostRequest req
    ) {
        postService.update(userId, postId, req);
        return ApiResponse.success(null);
    }

    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(
            @UserId Long userId,
            @PathVariable Long postId
    ) {
        postService.delete(userId, postId);
        return ApiResponse.success(null);
    }


    @Operation(summary = "게시글 상세 조회", description = "게시글의 상세 내용과 첨부파일 목록, 좋아요/북마크 여부 등을 조회합니다.")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getDetail(
            @UserId Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(postService.getDetail(userId, postId));
    }

    //좋아요
    @Operation(summary = "게시글 좋아요 (토글 방식)", description = "게시글의 좋아요 상태를 반전(Toggle)시킵니다. 현재 상태와 총 좋아요 수를 반환합니다.")
    @PostMapping("/{postId}/likes")
    public ApiResponse<ToggleLikeResponse> toggleLike(
            @UserId Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(postService.toggleLike(userId, postId));
    }

    //댓글 채택
    @Operation(summary = "댓글 채택", description = "질문글(Q&A) 등에서 특정 댓글을 해결책으로 채택합니다. 작성자만 가능합니다.")
    @PostMapping("/{postId}/comments/{commentId}/accept")
    public ApiResponse<Void> accept(
            @UserId Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        postService.acceptComment(userId, postId, commentId);
        return ApiResponse.success(null);
    }

    //북마크
    @Operation(summary = "게시글 북마크 (토글 방식)", description = "게시글을 북마크에 추가하거나 제거(Toggle)합니다.")
    @PostMapping("/{postId}/bookmarks")
    public ApiResponse<ToggleBookmarkResponse> toggleBookmark(
            @UserId Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(postService.toggleBookmark(userId, postId));
    }

    //글 구매
    @Operation(summary = "유료 글 액세스 권한 구매", description = "포인트를 지불하여 유료 게시글의 열람 권한을 획득합니다.")
    @PostMapping("/{postId}/access/purchase")
    public ApiResponse<PurchasePostAccessResponse> purchaseAccess(
            @UserId Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(postService.purchasePostAccess(userId, postId));
    }

    @Operation(
            summary = "첨부파일 업로드용 Presigned URL 발급",
            description = """
                게시글에 포함될 파일을 S3에 업로드하기 위한 Presigned URL을 발급합니다.
                - 업로드는 temp 경로로 수행됩니다.
                - 게시글 저장/수정 시 consume되어 최종 경로로 이동됩니다.
                - 규칙: items[0]은 썸네일(이미지 jpg/png/webp)가 권장됩니다.
                - 규칙: items[1..]은 일반 첨부(예: pdf 포함 허용)입니다.
                - 규칙: items[0]에 pdf가 들어오면 썸네일은 null입니다.
            """
    )
    @PostMapping("/uploads/presign")
    public ApiResponse<PresignUploadBatchResponse> presignAttachmentUpload(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadBatchRequest req
    ) {
        return ApiResponse.success(postAttachmentsService.presignAttachmentsBatch(userId, req));
    }

    @Operation(summary = "첨부파일 다운로드 URL 발급", description = "게시글의 첨부파일을 다운로드하기 위한 임시 URL을 발급받습니다.")
    @GetMapping("/{postId}/attachments/{attachmentId}/download-url")
    public ApiResponse<PresignDownloadResponse> downloadUrl(
            @UserId Long userId,
            @PathVariable Long postId,
            @PathVariable Long attachmentId
    ) {
        return ApiResponse.success(
                postAttachmentDownloadService.presignDownload(userId, postId, attachmentId)
        );
    }
}