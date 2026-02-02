package CamNecT.CamNecT_Server.domain.profile.dto.response;

import CamNecT.CamNecT_Server.domain.certificate.dto.response.CertificateResponse;
import CamNecT.CamNecT_Server.domain.education.dto.response.EducationResponse;
import CamNecT.CamNecT_Server.domain.experience.dto.response.ExperienceResponse;
import CamNecT.CamNecT_Server.domain.portfolio.dto.response.PortfolioPreviewResponse;
import CamNecT.CamNecT_Server.global.tag.model.TagAttributeName;

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
        List<TagDto> tags
) {
    public record ProfileBasicsDto(
            String bio,
            Boolean openToCoffeeChat,
            Boolean isFollowerVisible,
            String profileImageUrl,
            String studentNo,
            Integer yearLevel,
            Long institutionId,
            Long majorId
    ) {}

    public record TagDto(
            Long id,
            String name,
            String category,
            TagAttributeName attribute
    ) {}
}



