package CamNecT.CamNecT_Server.domain.auth.dto.signup;

public record SendSignupEmailResponse(
        String email,
        long expiresMinutes
) {}