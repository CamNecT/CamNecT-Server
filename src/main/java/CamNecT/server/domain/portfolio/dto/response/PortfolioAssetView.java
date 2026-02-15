package CamNecT.server.domain.portfolio.dto.response;

import java.time.LocalDateTime;

public record PortfolioAssetView(
        Long assetId,
        String type,
        String fileKey,
        String fileUrl,
        Integer sortOrder,
        LocalDateTime createdAt
) {}