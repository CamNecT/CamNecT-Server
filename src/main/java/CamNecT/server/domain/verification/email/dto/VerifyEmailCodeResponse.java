package CamNecT.server.domain.verification.email.dto;

public record VerifyEmailCodeResponse (
        Long userId,
        String verificationToken,
        long expiresMinutes
) {}
