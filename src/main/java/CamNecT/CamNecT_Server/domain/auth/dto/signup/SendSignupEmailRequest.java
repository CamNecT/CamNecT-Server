package CamNecT.CamNecT_Server.domain.auth.dto.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendSignupEmailRequest(
        @Email @NotBlank String email
) {}