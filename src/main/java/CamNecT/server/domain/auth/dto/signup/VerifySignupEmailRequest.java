package CamNecT.server.domain.auth.dto.signup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

// Ignore the obsolete signup field; phone registration belongs to My Page.
@JsonIgnoreProperties("phoneNum")
public record VerifySignupEmailRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code,

        @NotBlank @Size(max = 50) String username,
        @NotBlank String password,
        @NotBlank @Size(max = 100) String name,
        @NotNull Agreements agreements
) {
    public record Agreements(
            boolean serviceTerms,
            boolean privacyTerms
    ) {}
}
