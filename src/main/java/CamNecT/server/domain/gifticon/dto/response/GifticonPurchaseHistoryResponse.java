package CamNecT.server.domain.gifticon.dto.response;

import java.util.List;

public record GifticonPurchaseHistoryResponse(List<GifticonPurchaseSummary> items, Long nextCursor) {}
