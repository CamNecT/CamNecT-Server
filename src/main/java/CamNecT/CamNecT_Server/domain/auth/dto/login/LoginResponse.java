package CamNecT.CamNecT_Server.domain.auth.dto.login;

import CamNecT.CamNecT_Server.domain.auth.dto.LoginNextStep;

public record LoginResponse(
        String tokenType,     // "Bearer"
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs,
        long refreshTokenExpiresInMs,
        Long userId,
        String status,         // ADMIN_PENDING / ACTIVE ...
        String role,
        LoginNextStep nextStep// HOME / EMAIL_REVERIFY / DOCUMENT_REQUIRED / ONBOARDING_REQUIRED / DOCUMENT_REVIEW_WAITING / VERIFICATION_COMPLETE / ADMIN_DASHBOAR
) {
}
