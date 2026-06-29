package CamNecT.server.domain.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendPasswordResetEmailRequest(
        @Email @NotBlank String email
) {}
