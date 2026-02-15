package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.activity.model.enums.ActivityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityPreviewResponse(
        Long activityId,
        String title,
        String contextPreview,
        String thumbnailUrl,
        List<String> tags,
        Long bookmarkCount,
        String organizer,
        LocalDate applyEndDate,
        ActivityStatus status,
        LocalDateTime createdAt
) {
}
