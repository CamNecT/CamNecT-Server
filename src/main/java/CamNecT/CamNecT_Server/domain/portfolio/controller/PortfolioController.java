package CamNecT.CamNecT_Server.domain.portfolio.controller;

import CamNecT.CamNecT_Server.domain.portfolio.dto.request.PortfolioRequest;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioDetailResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
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
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PresignEngine presignEngine;

    @GetMapping
    public List<PortfolioPreviewResponse> portfolioPreview (@UserId Long userId){
        return portfolioService.portfolioPreview(userId);
    }

    @GetMapping("{portfolioId}")
    public PortfolioDetailResponse portfolioDetail (@UserId Long userId, @PathVariable Long portfolioId) {
        return portfolioService.portfolioDetail(userId, portfolioId);
    }

    @PostMapping
    public PortfolioPreviewResponse createPortfolio (@UserId Long userId, @RequestBody @Valid PortfolioRequest portfolioRequest){

        return portfolioService.create(userId, portfolioRequest);
    }

    //todo : 수정 삭제 로직 구현


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