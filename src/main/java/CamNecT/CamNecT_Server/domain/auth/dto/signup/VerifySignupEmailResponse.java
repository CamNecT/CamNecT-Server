package CamNecT.CamNecT_Server.domain.auth.dto.signup;

public record VerifySignupEmailResponse(
        Long userId,
        boolean alreadyVerified, // idempotent 처리 구분
        String tempToken,        // 필요하면 null 가능
        long expiresMinutes
) {}