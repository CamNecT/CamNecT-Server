package CamNecT.CamNecT_Server.domain.gifticon.repository;


import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GifticonProductRepository extends JpaRepository<GifticonProduct, Long> {

    Optional<GifticonProduct> findByVendorProductCode(String vendorProductCode);

    List<GifticonProduct> findAllByIsActiveTrueOrderBySortScoreDescIdDesc();

    List<GifticonProduct> findAllByIsActiveTrueOrderByPricePointsAscIdDesc();

    List<GifticonProduct> findAllByIsActiveTrueOrderByPricePointsDescIdDesc();
}