package CamNecT.server.domain.portfolio.dto.response;

import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.portfolio.dto.PortfolioProjectDto;

import java.util.List;

public record PortfolioDetailResponse(
        AuthorDto author,
        PortfolioProjectDto portfolio,
        List<PortfolioAssetView> portfolioAssets
) {
}
