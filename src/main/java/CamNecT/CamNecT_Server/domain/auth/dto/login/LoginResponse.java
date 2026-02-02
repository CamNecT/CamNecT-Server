package CamNecT.CamNecT_Server.domain.auth.dto.login;

public record LoginResponse(
        String tokenType,     // "Bearer"
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs,
        long refreshTokenExpiresInMs,
        Long userId,
        String status,         // ADMIN_PENDING / ACTIVE ...
        String role,

        LoginNextStep nextStep,     // HOME / EMAIL_REVERIFY / DOCUMENT_REQUIRED / ONBOARDING_REQUIRED / DOCUMENT_REVIEW_WAITING / VERIFICATION_COMPLETE / ADMIN_DASHBOARD
        String docStatus,     // 최신 제출 VerificationStatus.name() or null
        Long latestSubmissionId,
        String rejectReason,  // 최신 제출 rejectReason or null
        boolean onboardingDone
) {
}
