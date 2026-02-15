package CamNecT.server.domain.profile.components.education.dto.request;

import CamNecT.server.domain.profile.components.education.model.EducationStatus;

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