package CamNecT.server.domain.portfolio.dto.response;

import CamNecT.server.domain.portfolio.model.PortfolioProject;

public record PortfolioPreviewResponse(
        Long portfolioId,
        String title,
        String thumbnailUrl,
        boolean isPublic,
        boolean isFavorite
) {
    public static PortfolioPreviewResponse of(PortfolioProject project) {
        return new PortfolioPreviewResponse(
                project.getPortfolioId(),
                project.getTitle(),
                project.getThumbnailUrl(),
                project.isPublic(),
                project.isFavorite()
        );
    }
}