package CamNecT.server.domain.auth.dto.signup;

public record VerifySignupEmailResponse(
        Long userId,
        boolean alreadyVerified,
        String tempToken,
        long expiresMinutes
) {}
