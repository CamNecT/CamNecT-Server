package CamNecT.server.domain.verification.email.event;

public record EmailVerificationCodeIssuedEvent(String email, String code, long expiresMinutes) {}