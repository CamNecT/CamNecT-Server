package CamNecT.server.domain.auth.dto.login;

import CamNecT.server.domain.auth.dto.LoginNextStep;

public record LoginResponse(
        String tokenType,     // "Bearer"
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs,
        long refreshTokenExpiresInMs,
        Long userId,
        String status,         // ADMIN_PENDING / ACTIVE / SUSPENDED / WITHDRAWN
        String role,
        LoginNextStep nextStep // HOME / DOCUMENT_REQUIRED / DOCUMENT_REVIEW_WAITING / VERIFICATION_COMPLETE / ADMIN_DASHBOARD
) {
}
