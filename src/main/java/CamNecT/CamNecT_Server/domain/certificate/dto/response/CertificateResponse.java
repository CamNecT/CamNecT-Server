package CamNecT.CamNecT_Server.domain.certificate.dto.response;

import CamNecT.CamNecT_Server.domain.certificate.model.Certificate;

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
