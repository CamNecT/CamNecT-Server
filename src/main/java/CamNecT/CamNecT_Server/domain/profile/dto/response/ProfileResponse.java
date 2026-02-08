package CamNecT.CamNecT_Server.domain.profile.dto.response;

import CamNecT.CamNecT_Server.domain.profile.components.certificate.dto.response.CertificateResponse;
import CamNecT.CamNecT_Server.domain.profile.components.education.dto.response.EducationResponse;
import CamNecT.CamNecT_Server.domain.profile.components.experience.dto.response.ExperienceResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;

import java.util.List;

public record ProfileResponse(
        Long userId,
        String name,
        ProfileBasicsDto basics,
        int following,
        int follower,
        List<PortfolioPreviewResponse> portfolioProjectList,
        List<EducationResponse> educations,
        List<ExperienceResponse> experience,
        List<CertificateResponse> certificate,
        List<ProfileTagDto> tags
) {
    public record ProfileBasicsDto(
            String bio,
            Boolean openToCoffeeChat,
            Boolean isFollowerVisible,
            Boolean isEducationVisible,
            Boolean isExperienceVisible,
            Boolean isCertificateVisible,
            String profileImageUrl,
            String studentNo,
//            Integer yearLevel,
            Long institutionId,
            Long majorId
    ) {
    }
}



