package CamNecT.server.domain.gifticon.repository;


import CamNecT.server.domain.gifticon.model.GifticonProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GifticonProductRepository extends JpaRepository<GifticonProduct, Long> {

    List<GifticonProduct> findAllByIsActiveTrueOrderBySortScoreDescIdDesc();

    List<GifticonProduct> findAllByIsActiveTrueOrderByPricePointsAscIdDesc();

    List<GifticonProduct> findAllByIsActiveTrueOrderByPricePointsDescIdDesc();
}