package CamNecT.CamNecT_Server.domain.portfolio.controller;

import CamNecT.CamNecT_Server.domain.portfolio.dto.request.PortfolioRequest;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioDetailResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioResponse;
import CamNecT.CamNecT_Server.domain.portfolio.service.PortfolioAttachmentService;
import CamNecT.CamNecT_Server.domain.portfolio.service.PortfolioService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadBatchRequest;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadBatchResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Portfolio", description = "포트폴리오 관리 관련 API")
@RestController
@RequestMapping("/api/portfolio/{portfolioUserId}")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAttachmentService portfolioAttachmentService;

    @Operation(summary = "포트폴리오 목록 조회", description = "해당 사용자의 포트폴리오 요약 목록(Preview)을 조회합니다.")
    @GetMapping
    public ApiResponse<PortfolioResponse<List<PortfolioPreviewResponse>>> portfolioPreview (@UserId Long userId, @PathVariable Long portfolioUserId){
        return ApiResponse.success(portfolioService.portfolioPreview(userId, portfolioUserId));
    }

    @Operation(summary = "포트폴리오 상세 조회", description = "특정 포트폴리오의 상세 정보를 조회합니다.")
    @GetMapping("/{portfolioId}")
    public ApiResponse<PortfolioResponse<PortfolioDetailResponse>> portfolioDetail (@UserId Long userId, @PathVariable Long portfolioUserId,@PathVariable Long portfolioId) {
        return ApiResponse.success(portfolioService.portfolioDetail(userId, portfolioUserId,portfolioId));
    }

    @Operation(summary = "포트폴리오 생성", description = "새로운 포트폴리오를 등록합니다. 미리 발급받은 S3 Key를 포함해야 합니다.")
    @PostMapping
    public ApiResponse<PortfolioPreviewResponse> createPortfolio (@UserId Long userId, @PathVariable Long portfolioUserId, @RequestBody @Valid PortfolioRequest portfolioRequest){

        return ApiResponse.success(portfolioService.create(userId, portfolioUserId, portfolioRequest));
    }

    @Operation(summary = "포트폴리오 수정", description = "기존 포트폴리오의 정보를 수정합니다.")
    @PatchMapping("/{portfolioId}")
    public ApiResponse<PortfolioPreviewResponse> updatePortfolio (@UserId Long userId, @PathVariable Long portfolioId, @PathVariable Long portfolioUserId, @RequestBody @Valid PortfolioRequest portfolioRequest){
        return ApiResponse.success(portfolioService.update(userId, portfolioUserId, portfolioId,portfolioRequest));
    }

    @Operation(summary = "포트폴리오 삭제", description = "특정 포트폴리오를 삭제합니다.")
    @DeleteMapping("/{portfolioId}")
    public ApiResponse<String> deletePortfolio(@UserId Long userId, @PathVariable Long portfolioUserId, @PathVariable Long portfolioId){
        portfolioService.delete(userId, portfolioUserId, portfolioId);
        return ApiResponse.success("포트폴리오가 삭제되었습니다.");
    }

    // 공개 여부 설정
    @Operation(summary = "공개 여부 설정 (토글 방식)", description = "포트폴리오의 공개/비공개 상태를 반전(Toggle)시킵니다. 호출 시마다 상태가 변하며 결과값으로 최종 상태를 반환합니다.")
    @PatchMapping("/{portfolioId}/public")
    public ApiResponse<Boolean> togglePublic(
            @UserId Long userId,
            @PathVariable Long portfolioUserId,
            @PathVariable Long portfolioId
    ) {
        return ApiResponse.success(portfolioService.togglePublic(userId, portfolioUserId, portfolioId));
    }

    // 즐겨찾기 설정
    @Operation(summary = "즐겨찾기 설정 (토글 방식)", description = "포트폴리오의 즐겨찾기 등록/해제 상태를 반전(Toggle)시킵니다. 호출 시마다 상태가 변하며 결과값으로 최종 상태를 반환합니다.")
    @PatchMapping("/{portfolioId}/favorite")
    public ApiResponse<Boolean> toggleFavorite(
            @UserId Long userId,
            @PathVariable Long portfolioUserId,
            @PathVariable Long portfolioId
    ) {
        return ApiResponse.success(portfolioService.toggleFavorite(userId, portfolioUserId, portfolioId));
    }

    @Operation(summary = "썸네일 업로드용 Presigned URL 발급", description = "포트폴리오 대표 이미지를 업로드하기 위한 Presigned URL을 발급합니다. (이미지 파일만 허용)")
    @PostMapping("/uploads/presign/thumbnail")
    public ApiResponse<PresignUploadResponse> presignThumbnail(
            @UserId Long userId,
            @PathVariable Long portfolioUserId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        return ApiResponse.success(portfolioAttachmentService.presignThumbnail(userId, portfolioUserId, req));
    }

    @Operation(summary = "포트폴리오 첨부파일 업로드용 Presigned URL 발급(다건)", description = "포트폴리오 첨부파일을 업로드하기 위한 Presigned URL을 다건 발급합니다.")
    @PostMapping("/uploads/presign/assets")
    public ApiResponse<PresignUploadBatchResponse> presignAssetsBatch(
            @UserId Long userId,
            @PathVariable Long portfolioUserId,
            @RequestBody @Valid PresignUploadBatchRequest req
    ) {
        return ApiResponse.success(portfolioAttachmentService.presignAssetsBatch(userId, portfolioUserId, req));
    }

}