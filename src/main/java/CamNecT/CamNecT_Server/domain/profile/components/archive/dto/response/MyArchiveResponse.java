package CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    public enum Tab { COMMUNITY, EXTERNAL, RECRUITMENT }
    public enum Sort { RECOMMENDED, LATEST }

    public sealed interface Item permits CommunityItem, ExternalActivityItem, RecruitmentItem {}

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
    ) implements Item {}

    public record Author(
            Long userId,
            String name,
            String majorName,
            Integer yearLevel
    ) {}

    // 대외활동 카드
    public record ExternalActivityItem(
            Long activityId,
            List<String> tags,
            String title,
            String preview,
            long bookmarkCount,
            Integer dDay,
            LocalDateTime createdAt,
            String thumbnailUrl
    ) implements Item {}

    // 팀원모집 카드
    public record RecruitmentItem(
            Long recruitId,
            String recruitStatus,
            String title,
            String authorName,
            String activityTitle,
            long bookmarkCount,
            LocalDateTime createdAt
    ) implements Item {}
}