package CamNecT.server.domain.activity.dto.request;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AdminActivityRequest(
        @NotNull(message = "카테고리는 필수입니다.")
        ActivityCategory category,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        List<Long> tagIds,

        String organizer,

        String targetDescription,

        LocalDate applyStartDate,

        LocalDate applyEndDate,

        LocalDate resultAnnounceDate,

        String officialUrl,

        String thumbnailKey,

        String contextTitle,

        String content
) {
}