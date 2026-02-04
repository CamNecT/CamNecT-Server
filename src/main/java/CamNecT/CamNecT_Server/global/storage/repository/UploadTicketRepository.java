package CamNecT.CamNecT_Server.global.storage.repository;

import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UploadTicketRepository extends JpaRepository<UploadTicket, Long> {
    Optional<UploadTicket> findByStorageKey(String storageKey);

    @Modifying
    @Query(value = """
        UPDATE upload_tickets
        SET status = 'EXPIRED'
        WHERE status = 'PENDING'
          AND expires_at < :now
        """, nativeQuery = true)
    int bulkExpirePending(@Param("now") LocalDateTime now);

    @Query(value = """
        SELECT COUNT(*)
        FROM upload_tickets
        WHERE user_id = :userId
          AND purpose = :purpose
          AND status = 'PENDING'
          AND expires_at > :now
        """, nativeQuery = true)
    long countActivePending(@Param("userId") Long userId,
                            @Param("purpose") String purpose,
                            @Param("now") LocalDateTime now);

    long countByUserIdAndPurposeAndStatus(Long userId, UploadPurpose purpose, UploadTicket.Status status);

    long countByUserIdAndPurposeAndStatusAndExpiresAtAfter(
            Long userId, UploadPurpose purpose, UploadTicket.Status status, LocalDateTime now
    );

}