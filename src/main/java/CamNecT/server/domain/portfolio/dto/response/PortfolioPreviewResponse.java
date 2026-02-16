package CamNecT.server.domain.portfolio.dto.response;

import CamNecT.server.domain.portfolio.model.PortfolioProject;

import java.time.LocalDate;

public record PortfolioPreviewResponse(
        Long portfolioId,
        String title,
        String thumbnailUrl,
        boolean isPublic,
        boolean isFavorite,
        LocalDate updatedAt
) {
    public static PortfolioPreviewResponse of(PortfolioProject project) {
        return new PortfolioPreviewResponse(
                project.getPortfolioId(),
                project.getTitle(),
                project.getThumbnailUrl(),
                project.isPublic(),
                project.isFavorite(),
                project.getUpdatedAt()
        );
    }
}