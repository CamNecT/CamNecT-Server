package CamNecT.server.domain.verification.document.repository;

import CamNecT.server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.server.domain.verification.document.model.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVerificationSubmissionRepository extends JpaRepository<DocumentVerificationSubmission, Long> {


    List<DocumentVerificationSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    Optional<DocumentVerificationSubmission> findByIdAndUserId(Long id, Long userId);

    Page<DocumentVerificationSubmission> findByStatusOrderBySubmittedAtDesc(VerificationStatus status, Pageable pageable);

    Optional<DocumentVerificationSubmission> findTopByUserIdOrderBySubmittedAtDesc(Long userId);

    Optional<DocumentVerificationSubmission> findTopByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, VerificationStatus status);
}