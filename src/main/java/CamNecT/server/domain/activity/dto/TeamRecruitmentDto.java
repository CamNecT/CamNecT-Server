package CamNecT.server.domain.activity.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeamRecruitmentDto(
        Long recruitId,
        String activityTitle,
        String userName,
        String recruitStatus,
        String title,
        String content,
        LocalDate recruitDeadline,
        Integer recruitCount,
        long bookmarkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
