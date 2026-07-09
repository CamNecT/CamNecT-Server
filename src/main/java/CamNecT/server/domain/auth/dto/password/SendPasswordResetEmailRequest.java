package CamNecT.server.domain.auth.dto.password;

public record SendPasswordResetEmailRequest(
        String username,
        String email
) {}
