package CamNecT.server.domain.profile.components.certificate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CertificateRequest(
        @NotBlank @Size(max = 100) String certificateName,
        @NotNull LocalDate acquiredDate,
        @Size(max = 500) String credentialUrl
/*        String issuerName,
        LocalDate expireDate,
        String description*/
) {
}
