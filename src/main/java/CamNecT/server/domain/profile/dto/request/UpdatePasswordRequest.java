package CamNecT.server.domain.profile.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {}