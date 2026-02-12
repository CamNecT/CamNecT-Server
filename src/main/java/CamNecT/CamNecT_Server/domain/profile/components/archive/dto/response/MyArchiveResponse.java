package CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response;

import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyArchiveResponse(
        Tab tab,
        Sort sort,
        List<Item> items,
        boolean hasNext,
        Long nextCursorId,
        Long nextCursorValue
) {

    public enum Tab {COMMUNITY, EXTERNAL, RECRUITMENT}

    public enum Sort {RECOMMENDED, LATEST}

    public sealed interface Item permits CommunityItem, ExternalActivityItem, RecruitmentItem {
    }

    // 커뮤니티 카드
    public record CommunityItem(
            Long postId,
            String boardCode,
            List<String> tags,
            Author author,
            String title,
            String preview,
            long bookmarkCount,
            long commentCount,
            LocalDateTime createdAt,
            String thumbnailUrl
    ) implements Item {
    }

    public record Author(
            Long userId,
            String name,
            String majorName
    ) {
    }

    // 대외활동 카드
    public record ExternalActivityItem(
            Long activityId,
            String title,
            String contextPreview,
            String thumbnailUrl,
            List<String> tags,
            Long bookmarkCount,
            String organizer,
            LocalDate applyEndDate,
            ActivityStatus status,
            LocalDateTime createdAt,
            ActivityCategory category   // 추가
    ) implements Item {
    }

    // 팀원모집 카드
    public record RecruitmentItem(
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
    ) implements Item {
    }
}