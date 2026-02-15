package CamNecT.server.domain.activity.dto.request;

import java.time.LocalDate;

public record RecruitmentRequest(
        Long activityId,
        String title,
        LocalDate recruitDeadline,
        Integer recruitCount,
        String content
) {
}
