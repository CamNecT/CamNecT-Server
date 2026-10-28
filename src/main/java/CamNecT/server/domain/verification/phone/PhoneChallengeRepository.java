package CamNecT.server.domain.verification.phone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PhoneChallengeRepository extends JpaRepository<PhoneChallenge, Long> {
    @Modifying
    @Query("delete from PhoneChallenge c where c.expiresAt <= :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
