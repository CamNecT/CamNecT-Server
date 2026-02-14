package CamNecT.server.domain.gifticon.dto.response;

import java.time.LocalDateTime;

public record GifticonPurchaseConfirmResponse(
        Long purchaseId,
        LocalDateTime requestedAt
) {}