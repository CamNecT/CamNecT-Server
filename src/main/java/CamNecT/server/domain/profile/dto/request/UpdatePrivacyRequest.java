package CamNecT.server.domain.profile.dto.request;

public record UpdatePrivacyRequest(
        Boolean isFollowerVisible,
        Boolean isEducationVisible,
        Boolean isExperienceVisible,
        Boolean isCertificateVisible
) {
}