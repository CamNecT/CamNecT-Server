package CamNecT.server.domain.portfolio.repository;

import CamNecT.server.domain.portfolio.model.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {

    @Query("SELECT pa FROM PortfolioAsset pa " +
            "WHERE pa.portfolioProject.portfolioId = :portfolioId " +
            "ORDER BY pa.sortOrder ASC")
    List<PortfolioAsset> findAssetsByPortfolioId(@Param("portfolioId") Long portfolioId);

}
