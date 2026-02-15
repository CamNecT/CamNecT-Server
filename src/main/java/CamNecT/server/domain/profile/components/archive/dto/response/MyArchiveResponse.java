package CamNecT.server.domain.profile.components.archive.dto.response;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.enums.ActivityStatus;
import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.community.dto.response.PostSummaryResponse;
import CamNecT.server.domain.community.model.enums.ContentAccessStatus;
import CamNecT.server.domain.community.model.enums.PostAccessType;
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
    public enum ArchiveKind {MY_POSTS, BOOKMARKS}

    public enum Tab {COMMUNITY, EXTERNAL, RECRUITMENT}

    public enum Sort {RECOMMENDED, LATEST}

    public sealed interface Item permits CommunityItem, ExternalActivityItem, RecruitmentItem {
    }

    // 커뮤니티 카드
    public record CommunityItem(
            Long postId,
            String boardCode,
            String title,
            String preview,
            LocalDateTime createdAt,
            long likeCount,
            long answerCount,
            long commentCount,
            long bookmarkCount,
            boolean acceptedBadge,
            List<String> tags,
            AuthorDto author,
            String thumbnailUrl,
            PostAccessType accessType,
            ContentAccessStatus accessStatus
    ) implements Item {

        public static CommunityItem from(PostSummaryResponse r) {
            return new CommunityItem(
                    r.postId(),
                    r.boardCode().name(), // 필요하면 getCode()로
                    r.title(),
                    r.preview(),
                    r.createdAt(),
                    r.likeCount(),
                    r.answerCount(),
                    r.commentCount(),
                    r.bookmarkCount(),
                    r.acceptedBadge(),
                    r.tags(),
                    r.author(),
                    r.thumbnailUrl(),
                    r.accessType(),
                    r.accessStatus()
            );
        }
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