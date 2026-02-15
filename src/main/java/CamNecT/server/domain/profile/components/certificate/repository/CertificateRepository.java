package CamNecT.server.domain.profile.components.certificate.repository;

import CamNecT.server.domain.profile.components.certificate.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findAllByUser_UserIdOrderByAcquiredDateDesc(Long userId);

    void deleteByUser_UserId(Long userId);
}