package CamNecT.CamNecT_Server.domain.gifticon.dto.response;

public record GifticonProductDetailResponse(
        Long productId,
        String brandName,
        String productName,
        Integer pricePoints,
        String imageUrl,
        String detailImageUrl,
        boolean bookmarked,
        boolean active
) {}