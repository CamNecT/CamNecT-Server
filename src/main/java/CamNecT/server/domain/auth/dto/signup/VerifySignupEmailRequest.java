package CamNecT.server.domain.auth.dto.signup;

import jakarta.validation.constraints.*;
import CamNecT.server.global.common.util.PhoneNumbers;
import io.swagger.v3.oas.annotations.media.Schema;

public record VerifySignupEmailRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code,

        @NotBlank @Size(max = 50) String username,
        @NotBlank String password,
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "가입 휴대전화번호. 공백·하이픈 제거 후 저장하며 기프티콘 기본 수신 번호로 사용합니다.")
        @NotBlank @Size(max = 20) @Pattern(regexp = PhoneNumbers.MOBILE_PATTERN) String phoneNum,
        @NotNull Agreements agreements
) {
    public VerifySignupEmailRequest {
        phoneNum = PhoneNumbers.normalize(phoneNum);
    }

    public record Agreements(
            boolean serviceTerms,
            boolean privacyTerms
    ) {}
}
