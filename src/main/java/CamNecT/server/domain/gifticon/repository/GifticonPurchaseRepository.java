package CamNecT.server.domain.gifticon.repository;

import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.gifticon.dto.response.GifticonPurchaseSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GifticonPurchaseRepository extends JpaRepository<GifticonPurchase, Long> {

    @Query("""
            select new CamNecT.server.domain.gifticon.dto.response.GifticonPurchaseSummary(
                p.id, product.id, product.productName, product.brandName, product.imageUrl,
                p.quantity, p.unitPricePoints, p.totalPricePoints, p.requestedAt, p.adminProcessedAt, p.adminSuccess)
            from GifticonPurchase p join p.product product
            where p.user.userId = :userId and (:cursor is null or p.id < :cursor)
            order by p.id desc
            """)
    List<GifticonPurchaseSummary> findHistory(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable page);

    Optional<GifticonPurchase> findByUser_UserIdAndClientRequestId(Long userId, String clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from GifticonPurchase p
            where p.exportBatch is null
            order by p.requestedAt asc, p.id asc
            """)
    List<GifticonPurchase> findNextUnbatchedForExport(Pageable pageable);

    @Query("""
            select p
            from GifticonPurchase p
            join fetch p.user
            join fetch p.product
            where p.exportBatch.id = :batchId
            order by p.requestedAt asc, p.id asc
            """)
    List<GifticonPurchase> findAllForExportBatch(@Param("batchId") Long batchId);
}
