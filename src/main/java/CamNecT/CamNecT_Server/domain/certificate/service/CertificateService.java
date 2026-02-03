package CamNecT.CamNecT_Server.domain.certificate.service;

import CamNecT.CamNecT_Server.domain.certificate.dto.request.CertificateRequest;
import CamNecT.CamNecT_Server.domain.certificate.dto.response.CertificateResponse;
import CamNecT.CamNecT_Server.domain.certificate.model.Certificate;
import CamNecT.CamNecT_Server.domain.certificate.repository.CertificateRepository;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addCertificate(Long userId, CertificateRequest request) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Certificate certificate = Certificate.builder()
                .user(user)
                .certificateName(request.certificateName())
                .issuerName(request.issuerName()).
                acquiredDate(request.acquiredDate())
                .expireDate(request.expireDate())
                .credentialUrl(request.credentialUrl())
                .description(request.description())
                .build();

        certificateRepository.save(certificate);
    }

    public List<CertificateResponse> getMyCertificate(Long userId) {
        return certificateRepository.findAllByUser_UserIdOrderByAcquiredDateDesc(userId)
                .stream()
                .map(CertificateResponse::from)
                .toList();
    }

    @Transactional
    public void updateCertificate(Long userId, Long certificateId, CertificateRequest request) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CustomException(UserErrorCode.CERTIFICATE_NOT_FOUND));

        // 본인 확인
        if (!certificate.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.CERTIFICATE_FORBIDDEN);
        }

        certificate.updateCertificate(
                request.certificateName(),
                request.issuerName(),
                request.acquiredDate(),
                request.expireDate(),
                request.credentialUrl(),
                request.description()
        );
    }

    @Transactional
    public void deleteCertificate(Long userId, Long certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new CustomException(UserErrorCode.CERTIFICATE_NOT_FOUND));

        if (!certificate.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.CERTIFICATE_FORBIDDEN);
        }

        certificateRepository.delete(certificate);
    }
}
