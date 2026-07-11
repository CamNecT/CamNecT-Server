package CamNecT.server.domain.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPasswordResetEmailRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
