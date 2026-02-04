package CamNecT.CamNecT_Server.domain.experience.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
        Long userId,
        String companyName,
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent,
        List<String> responsibilities) {
}