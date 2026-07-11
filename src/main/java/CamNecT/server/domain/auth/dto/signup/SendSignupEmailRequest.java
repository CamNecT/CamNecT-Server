package CamNecT.server.domain.auth.dto.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendSignupEmailRequest(
        @Email @NotBlank @Size(max = 255) String email
) {}
