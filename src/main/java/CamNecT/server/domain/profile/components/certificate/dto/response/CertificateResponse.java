package CamNecT.server.domain.profile.components.certificate.dto.response;

import CamNecT.server.domain.profile.components.certificate.model.Certificate;

import java.time.LocalDate;

public record CertificateResponse(
        Long certificateId,
        String certificateName,
        LocalDate acquiredDate,
        String credentialUrl
//        String issuerName,
//        LocalDate expireDate,
//        String description
) {
    public static CertificateResponse from(Certificate certificate) {
        return new CertificateResponse(
                certificate.getCertificateId(),
                certificate.getCertificateName(),
                certificate.getAcquiredDate(),
                certificate.getCredentialUrl()
//                certificate.getIssuerName(),
//                certificate.getExpireDate(),
//                certificate.getDescription()
        );
    }
}
