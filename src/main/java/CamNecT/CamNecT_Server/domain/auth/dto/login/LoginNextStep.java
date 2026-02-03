package CamNecT.CamNecT_Server.domain.auth.dto.login;

public enum LoginNextStep {
    HOME,
    EMAIL_REVERIFY,
    DOCUMENT_REQUIRED,
    ONBOARDING_REQUIRED,
    DOCUMENT_REVIEW_WAITING,
    VERIFICATION_COMPLETE,
    ADMIN_DASHBOARD
}