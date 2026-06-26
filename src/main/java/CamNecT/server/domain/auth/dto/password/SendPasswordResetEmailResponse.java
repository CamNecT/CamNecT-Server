package CamNecT.server.domain.auth.dto.password;

public record SendPasswordResetEmailResponse(
        String email,
        long expiresMinutes
) {}
