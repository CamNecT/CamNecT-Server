package CamNecT.CamNecT_Server.domain.gifticon.controller;

import CamNecT.CamNecT_Server.domain.gifticon.dto.request.ConfirmGifticonPurchaseRequest;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.*;
import CamNecT.CamNecT_Server.domain.gifticon.service.GifticonPurchaseService;
import CamNecT.CamNecT_Server.domain.gifticon.service.GifticonService;
import CamNecT.CamNecT_Server.domain.gifticon.service.GifticonService.Sort;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gifticons")
public class GifticonController {

    private final GifticonService gifticonService;
    private final GifticonPurchaseService purchaseService;

    @Operation(summary = "기프티콘 샵 홈(상품목록+내포인트)", description = "서버 캐시 DB의 상품목록과 사용자 포인트를 한번에 내려줍니다.")
    @GetMapping("/home")
    public ApiResponse<GifticonHomeResponse> home(
            @UserId Long userId,
            @RequestParam(defaultValue = "POPULAR") Sort sort
    ) {
        return ApiResponse.success(gifticonService.home(userId, sort));
    }

    @Operation(summary = "상품 상세", description = "상품 상세 정보를 조회합니다.")
    @GetMapping("/products/{productId}")
    public ApiResponse<GifticonProductDetailResponse> detail(
            @UserId Long userId,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(gifticonService.productDetail(userId, productId));
    }

    @Operation(summary = "구매 확정", description = "프론트 최종확인 이후 서버에 구매확정을 보내고 포인트 차감 + 구매요청 DB 적재를 수행합니다.")
    @PostMapping("/purchases/confirm")
    public ApiResponse<GifticonPurchaseConfirmResponse> confirm(
            @UserId Long userId,
            @RequestBody @Valid ConfirmGifticonPurchaseRequest req
    ) {
        return ApiResponse.success(purchaseService.confirm(userId, req));
    }
}