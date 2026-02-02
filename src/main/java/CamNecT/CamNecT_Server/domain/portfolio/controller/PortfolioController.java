package CamNecT.CamNecT_Server.domain.portfolio.controller;

import CamNecT.CamNecT_Server.domain.portfolio.dto.request.PortfolioRequest;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioDetailResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioResponse;
import CamNecT.CamNecT_Server.domain.portfolio.service.PortfolioService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.service.PresignEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/{portfolioUserId}")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PresignEngine presignEngine;

    @GetMapping
    public ApiResponse<PortfolioResponse<List<PortfolioPreviewResponse>>> portfolioPreview (@UserId Long userId, @PathVariable Long portfolioUserId){
        return ApiResponse.success(portfolioService.portfolioPreview(userId, portfolioUserId));
    }

    @GetMapping("/{portfolioId}")
    public ApiResponse<PortfolioResponse<PortfolioDetailResponse>> portfolioDetail (@UserId Long userId, @PathVariable Long portfolioUserId,@PathVariable Long portfolioId) {
        return ApiResponse.success(portfolioService.portfolioDetail(userId, portfolioUserId,portfolioId));
    }

    @PostMapping
    public ApiResponse<PortfolioPreviewResponse> createPortfolio (@UserId Long userId, @PathVariable Long portfolioUserId, @RequestBody @Valid PortfolioRequest portfolioRequest){

        return ApiResponse.success(portfolioService.create(userId, portfolioUserId, portfolioRequest));
    }

    @PatchMapping("/{portfolioId}")
    public ApiResponse<PortfolioPreviewResponse> updatePortfolio (@UserId Long userId, @PathVariable Long portfolioId, @RequestBody @Valid PortfolioRequest portfolioRequest){
        return ApiResponse.success(portfolioService.update(userId, portfolioId,portfolioRequest));
    }

    @DeleteMapping("/{portfolioId}")
    public ApiResponse<String> deletePortfolio(@UserId Long userId, @PathVariable Long portfolioId){
        portfolioService.delete(userId, portfolioId);
        return ApiResponse.success("포트폴리오가 삭제되었습니다.");
    }

    // 공개 여부 설정
    @PatchMapping("/{portfolioId}/public")
    public ApiResponse<Boolean> togglePublic(
            @UserId Long userId,
            @PathVariable Long portfolioId
    ) {
        return ApiResponse.success(portfolioService.togglePublic(userId, portfolioId));
    }

    // 즐겨찾기 설정
    @PatchMapping("/{portfolioId}/favorite")
    public ApiResponse<Boolean> toggleFavorite(
            @UserId Long userId,
            @PathVariable Long portfolioId
    ) {
        return ApiResponse.success(portfolioService.toggleFavorite(userId, portfolioId));
    }


    @PostMapping("/uploads/presign/thumbnail")
    public ApiResponse<PresignUploadResponse> presignThumbnail(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        String keyPrefix = "portfolio/user-" + userId + "/thumbnail";
        return ApiResponse.success(
                presignEngine.issueUpload(
                        userId,
                        UploadPurpose.PORTFOLIO_ATTACHMENT,
                        keyPrefix,
                        req.contentType(),
                        req.size(),
                        req.originalFilename()
                )
        );
    }

    @PostMapping("/uploads/presign/asset")
    public ApiResponse<PresignUploadResponse> presignAsset(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        String keyPrefix = "portfolio/user-" + userId + "/assets";
        return ApiResponse.success(
                presignEngine.issueUpload(
                        userId,
                        UploadPurpose.PORTFOLIO_ATTACHMENT,
                        keyPrefix,
                        req.contentType(),
                        req.size(),
                        req.originalFilename()
                )
        );
    }

}