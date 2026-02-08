package CamNecT.CamNecT_Server.domain.gifticon.repository;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GifticonPurchaseRepository extends JpaRepository<GifticonPurchase, Long> {

    Optional<GifticonPurchase> findByUser_UserIdAndClientRequestId(Long userId, String clientRequestId);

    List<GifticonPurchase> findAllByUser_UserIdOrderByRequestedAtDesc(Long userId);

    List<GifticonPurchase> findAllByExportBatchIsNullOrderByRequestedAtAsc();
}