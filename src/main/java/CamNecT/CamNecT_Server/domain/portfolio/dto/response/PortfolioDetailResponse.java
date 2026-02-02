package CamNecT.CamNecT_Server.domain.portfolio.dto.response;

import CamNecT.CamNecT_Server.domain.portfolio.dto.PortfolioProjectDto;

import java.util.List;

public record PortfolioDetailResponse(
    Boolean isMine,
    PortfolioProjectDto portfolio,
    List<PortfolioAssetView> portfolioAssets
) {}
