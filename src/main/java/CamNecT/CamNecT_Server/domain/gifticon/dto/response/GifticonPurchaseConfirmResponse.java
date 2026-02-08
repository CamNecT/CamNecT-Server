package CamNecT.CamNecT_Server.domain.gifticon.dto.response;

import java.time.LocalDateTime;

public record GifticonPurchaseConfirmResponse(
        Long purchaseId,
        LocalDateTime requestedAt
) {}