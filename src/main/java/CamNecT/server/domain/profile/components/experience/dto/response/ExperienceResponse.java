package CamNecT.server.domain.profile.components.experience.dto.response;

import CamNecT.server.domain.profile.components.experience.model.Experience;

import java.time.LocalDate;
import java.util.List;

public record ExperienceResponse(
        Long experienceId,
        String companyName,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent,
        List<String> responsibilities) {
    public static ExperienceResponse from(Experience experience) {
        return new ExperienceResponse(
                experience.getExperienceId(),
                experience.getCompanyName(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getIsCurrent(),
                experience.getResponsibilities() != null ? experience.getResponsibilities() : List.of());
    }
}
