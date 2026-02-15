package CamNecT.server.domain.auth.dto.signup;

public record SendSignupEmailResponse(
        String email,
        long expiresMinutes
) {}