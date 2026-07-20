package CamNecT.server.domain.verification.email.repository;

import CamNecT.server.domain.verification.email.model.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findTopByEmailAndUsedAtIsNullOrderByIdDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationToken> findFirstByEmailAndUsedAtIsNullOrderByIdDesc(String email);

    void deleteByEmailAndUsedAtIsNull(String email);

    void deleteByUser_UserId(Long userId);
}
