package CamNecT.CamNecT_Server.domain.certificate.dto.request;

import java.time.LocalDate;

public record CertificateRequest(
        Long userId,
        String certificateName,
        LocalDate acquiredDate,
        String credentialUrl
//        String issuerName,
//        LocalDate expireDate,
//        String description
) {
}