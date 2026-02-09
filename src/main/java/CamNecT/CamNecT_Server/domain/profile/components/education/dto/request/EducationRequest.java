package CamNecT.CamNecT_Server.domain.profile.components.education.dto.request;

import CamNecT.CamNecT_Server.domain.profile.components.education.model.EducationStatus;

import java.time.LocalDate;

public record EducationRequest(
        Long institutionId,
//        Long majorId,
//        String degree,
        LocalDate startDate,
        LocalDate endDate,
        EducationStatus status,
        String description
) {}