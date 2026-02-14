package CamNecT.CamNecT_Server.domain.verification.email.repository;

import CamNecT.CamNecT_Server.domain.verification.email.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findTopByEmailAndUsedAtIsNullOrderByIdDesc(String email);

    void deleteByEmailAndUsedAtIsNull(String email);

    void deleteByUser_UserId(Long userId);
}
