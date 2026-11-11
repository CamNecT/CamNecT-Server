package CamNecT.server.domain.gifticon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GifticonPurchaseSummary(
        Long purchaseId, Long productId, String productName, String brandName, String imageUrl,
        int quantity, int unitPricePoints, int totalPricePoints, LocalDateTime requestedAt,
        LocalDateTime processedAt,
        @Schema(description = "관리자 처리 결과. 미확정이면 null, 성공이면 true, 실패이면 false")
        Boolean processingSuccess
) {}
