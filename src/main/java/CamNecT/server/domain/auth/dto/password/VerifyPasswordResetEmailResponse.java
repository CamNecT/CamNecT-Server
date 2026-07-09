package CamNecT.server.domain.auth.dto.password;

public record VerifyPasswordResetEmailResponse(
        String resetToken,
        long expiresMinutes
) {}
