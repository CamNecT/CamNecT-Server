package CamNecT.server.domain.activity.service;

import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.community.dto.response.CommentItemResponse;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import CamNecT.server.domain.community.service.AuthorAssembler;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.config.QuerydslConfig;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({ActivityCommentService.class, QuerydslConfig.class})
class ActivityCommentPagingIntegrationTest {
    @Autowired TestEntityManager entityManager;
    @Autowired ActivityCommentService service;
    @MockitoBean AccountAccessGuard accountAccessGuard;
    @MockitoBean AuthorAssembler authorAssembler;

    @Test
    void boundsEachThreadsPreviewAndContinuesWithoutGapsWhenTimestampsTie() {
        ExternalActivity activity = activity();
        ActivityComment olderRoot = comment(activity, null, CommentStatus.PUBLISHED, "older root");
        ActivityComment root = comment(activity, null, CommentStatus.PUBLISHED, "root");
        comment(activity, root, CommentStatus.HIDDEN, "hidden reply");
        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 65; i++) {
            expected.add(comment(activity, root, i == 22 ? CommentStatus.DELETED : CommentStatus.PUBLISHED,
                    "reply " + i).getId());
        }
        ActivityComment olderReply = comment(activity, olderRoot, CommentStatus.PUBLISHED, "ordinary reply");
        entityManager.flush();
        entityManager.getEntityManager().createNativeQuery("UPDATE activity_comments SET created_at = :timestamp WHERE activity_id = :activityId")
                .setParameter("timestamp", LocalDateTime.of(2026, 11, 11, 12, 0))
                .setParameter("activityId", activity.getActivityId()).executeUpdate();
        entityManager.clear();

        var first = service.list(1L, activity.getActivityId(), null, 2);
        assertThat(first.hasNext()).isFalse();
        assertThat(first.items()).hasSize(23);
        assertThat(first.items().getFirst().commentId()).isEqualTo(root.getId());
        assertThat(first.items().subList(1, 21)).extracting(CommentItemResponse::commentId)
                .containsExactlyElementsOf(expected.subList(0, 20));
        assertThat(first.items().subList(21, 23)).extracting(CommentItemResponse::commentId)
                .containsExactly(olderRoot.getId(), olderReply.getId());
        var continuation = first.replyPages().stream().filter(p -> p.parentCommentId().equals(root.getId())).findFirst().orElseThrow();
        assertThat(continuation.hasNext()).isTrue();
        assertThat(continuation.nextCursorId()).isEqualTo(expected.get(19));
        assertThat(first.replyPages().stream().filter(p -> p.parentCommentId().equals(olderRoot.getId())).findFirst().orElseThrow().hasNext()).isFalse();
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(first);
        assertThat(json.has("items")).isTrue();
        assertThat(json.has("nextCursorId")).isTrue();
        assertThat(json.has("hasNext")).isTrue();
        assertThat(json.at("/replyPages/0/parentCommentId").asLong()).isEqualTo(root.getId());
        var rootPage = service.list(1L, activity.getActivityId(), null, 1);
        assertThat(rootPage.nextCursorId()).isEqualTo(root.getId());
        assertThat(rootPage.hasNext()).isTrue();
        assertThat(service.list(1L, activity.getActivityId(), rootPage.nextCursorId(), 1).items())
                .extracting(CommentItemResponse::commentId).containsExactly(olderRoot.getId(), olderReply.getId());

        List<Long> seen = new ArrayList<>(expected.subList(0, 20));
        Long cursor = continuation.nextCursorId();
        do {
            var page = service.listReplies(1L, activity.getActivityId(), root.getId(), cursor, 20);
            assertThat(page.items()).hasSizeLessThanOrEqualTo(20);
            page.items().stream().filter(c -> c.commentId().equals(expected.get(22))).forEach(c -> {
                assertThat(c.content()).isEqualTo("삭제된 댓글입니다.");
                assertThat(c.author()).isNull();
            });
            seen.addAll(page.items().stream().map(CommentItemResponse::commentId).toList());
            cursor = page.nextCursorId();
            assertThat(page.hasNext()).isEqualTo(cursor != null);
        } while (cursor != null);
        assertThat(seen).containsExactlyElementsOf(expected);
    }

    @Test
    void rejectsReplyCursorsFromOtherThreadsAndRootsFromOtherActivities() {
        ExternalActivity activity = activity();
        ActivityComment root = comment(activity, null, CommentStatus.PUBLISHED, "root");
        ActivityComment otherRoot = comment(activity, null, CommentStatus.PUBLISHED, "other root");
        ActivityComment otherReply = comment(activity, otherRoot, CommentStatus.PUBLISHED, "other reply");
        ExternalActivity otherActivity = activity();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> service.listReplies(1L, activity.getActivityId(), root.getId(), otherReply.getId(), 20))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommunityErrorCode.INVALID_CURSOR));
        assertThatThrownBy(() -> service.listReplies(1L, otherActivity.getActivityId(), root.getId(), null, 20))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    private ExternalActivity activity() {
        return entityManager.persist(ExternalActivity.builder().title("club").category(ActivityCategory.CLUB).build());
    }

    private ActivityComment comment(ExternalActivity activity, ActivityComment parent, CommentStatus status, String content) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 11, 12, 0);
        return entityManager.persist(ActivityComment.builder().activity(activity).parent(parent).userId(1L)
                .content(content).status(status).createdAt(timestamp).updatedAt(timestamp).build());
    }
}
