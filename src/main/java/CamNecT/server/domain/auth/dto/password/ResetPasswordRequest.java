package CamNecT.server.domain.auth.dto.password;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank String newPassword
) {}
