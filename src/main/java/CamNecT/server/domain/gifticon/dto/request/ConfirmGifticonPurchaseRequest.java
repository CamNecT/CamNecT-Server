package CamNecT.server.domain.gifticon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmGifticonPurchaseRequest(
        @NotNull Long productId,
        @NotNull Integer quantity,
        @NotNull Integer spendPoints,
        @NotBlank @Size(max = 100) String clientRequestId,

        // 선물하기(현재는 틀만)
        @Size(max = 100) String recipientName,
        @Size(max = 30) String recipientPhone,
        @Size(max = 500) String giftMessage
) {}
