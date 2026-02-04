package CamNecT.CamNecT_Server.domain.profile.dto.request;

public record UpdatePrivacyRequest(
        Boolean isFollowerVisible,
        Boolean isEducationVisible,
        Boolean isExperienceVisible,
        Boolean isCertificateVisible
) {
}