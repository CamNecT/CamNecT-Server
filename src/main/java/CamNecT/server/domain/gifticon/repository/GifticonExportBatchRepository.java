package CamNecT.server.domain.gifticon.repository;

import CamNecT.server.domain.gifticon.model.GifticonExportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GifticonExportBatchRepository extends JpaRepository<GifticonExportBatch, Long> {

    Optional<GifticonExportBatch> findTopByOrderByExportedAtDesc();
}