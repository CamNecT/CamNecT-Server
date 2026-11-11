package CamNecT.server.global.point.repository;

import CamNecT.server.global.point.model.PointTransaction;
import CamNecT.server.global.point.model.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    @Query("""
            select tx from PointTransaction tx
            where tx.userId = :userId and (:type is null or tx.transactionType = :type)
              and (:cursor is null or tx.id < :cursor)
            order by tx.id desc
            """)
    List<PointTransaction> findHistory(Long userId, TransactionType type, Long cursor, Pageable page);
}
