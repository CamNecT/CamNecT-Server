package CamNecT.CamNecT_Server.domain.gifticon.repository;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonExportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GifticonExportBatchRepository extends JpaRepository<GifticonExportBatch, Long> {

    Optional<GifticonExportBatch> findTopByOrderByExportedAtDesc();
}