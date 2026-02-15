package CamNecT.server.domain.portfolio.dto.response;

import CamNecT.server.domain.portfolio.dto.PortfolioProjectDto;

import java.util.List;

public record PortfolioDetailResponse(
    PortfolioProjectDto portfolio,
    List<PortfolioAssetView> portfolioAssets
) {}
