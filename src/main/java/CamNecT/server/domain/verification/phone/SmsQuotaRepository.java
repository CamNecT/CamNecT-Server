package CamNecT.server.domain.verification.phone;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SmsQuotaRepository extends JpaRepository<SmsQuota, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from SmsQuota q where q.key = 'lock'")
    Optional<SmsQuota> lockBudget();

    // MySQL REPEATABLE READ must use a current read after waiting for the budget lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from SmsQuota q where q.key = :key")
    Optional<SmsQuota> findForUpdate(@Param("key") String key);

    @Modifying
    @Query("delete from SmsQuota q where q.key <> 'lock' and q.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
