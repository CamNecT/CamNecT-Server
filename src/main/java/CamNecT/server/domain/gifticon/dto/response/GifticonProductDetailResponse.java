package CamNecT.server.domain.gifticon.dto.response;

public record GifticonProductDetailResponse(
        Long productId,
        String brandName,
        String productName,
        Integer pricePoints,
        String imageUrl,
        boolean active
) {}