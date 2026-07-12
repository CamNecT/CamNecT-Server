package CamNecT.server.domain.activity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecruitmentRequest(
        @Positive Long activityId,
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate recruitDeadline,
        @NotNull @Min(1) Integer recruitCount,
        @NotBlank @Size(max = 16000) String content
) {
}
