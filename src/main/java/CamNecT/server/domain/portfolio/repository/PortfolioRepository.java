package CamNecT.server.domain.portfolio.repository;

import CamNecT.server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.server.domain.portfolio.model.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioProject, Long> {

    @Query("SELECT new CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse(p.portfolioId, p.title, p.thumbnailUrl, p.isPublic, p.isFavorite) " +
            "FROM PortfolioProject p " +
            "WHERE p.userId = :userId " +
            "ORDER BY p.createdAt DESC")
    List<PortfolioPreviewResponse> findPreviewsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT new CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse(p.portfolioId, p.title, p.thumbnailUrl, p.isPublic, p.isFavorite)
    FROM PortfolioProject p
    WHERE p.userId = :userId AND p.isPublic = true
    ORDER BY p.createdAt DESC
    """)
    List<PortfolioPreviewResponse> findPublicPreviewsByUserId(@Param("userId") Long userId);

}
