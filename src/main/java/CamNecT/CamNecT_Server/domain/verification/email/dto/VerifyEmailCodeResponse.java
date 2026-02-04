package CamNecT.CamNecT_Server.domain.verification.email.dto;

public record VerifyEmailCodeResponse (
        Long userId,
        String verificationToken,
        long expiresMinutes
) {}
