package CamNecT.CamNecT_Server.domain.activity.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityPreviewResponse(
        Long activityId,
        String title,
        String context,
        String thumbnailUrl,
        List<String> tags,
        Long bookmarkCount,
        String organizer,
        LocalDate applyEndDate,
        LocalDateTime createdAt
) {
}
