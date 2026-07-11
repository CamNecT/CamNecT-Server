package CamNecT.server.domain.auth.dto.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendSignupEmailRequest(
        @Email @NotBlank String email
) {}