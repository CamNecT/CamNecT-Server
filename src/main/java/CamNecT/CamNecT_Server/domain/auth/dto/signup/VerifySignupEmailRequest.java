package CamNecT.CamNecT_Server.domain.auth.dto.signup;

import jakarta.validation.constraints.*;

public record VerifySignupEmailRequest(
        @Email @NotBlank String email,
        @Pattern(regexp = "\\d{6}") String code,

        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String phoneNum,
        @NotNull Agreements agreements
) {
    public record Agreements(
            boolean serviceTerms,
            boolean privacyTerms
    ) {}
}