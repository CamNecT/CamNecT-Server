package CamNecT.server.domain.profile.components.experience.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
        @NotBlank @Size(max = 100) String companyName,
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,
        LocalDate endDate,
        @NotNull Boolean isCurrent,
        List<@NotBlank @Size(max = 16000) String> responsibilities) {

    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
