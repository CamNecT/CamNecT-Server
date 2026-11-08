package CamNecT.server.domain.activity.repository.comment;

import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.activity.model.comment.ActivityCommentLike;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import CamNecT.server.global.common.config.QuerydslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class ActivityCommentRepositoryTest {
    @Autowired TestEntityManager entityManager;
    @Autowired ExternalActivityRepository activities;
    @Autowired ActivityCommentRepository comments;
    @Autowired ActivityCommentLikeRepository likes;

    @Test
    void pagesRootsAndDeletesLikesAndRepliesBeforeActivity() {
        ExternalActivity activity = entityManager.persist(ExternalActivity.builder().title("동아리").category(ActivityCategory.CLUB).build());
        ActivityComment root = entityManager.persist(ActivityComment.create(activity, 1L, null, "댓글"));
        ActivityComment reply = entityManager.persist(ActivityComment.create(activity, 2L, root, "답글"));
        entityManager.persist(ActivityCommentLike.of(reply, 1L));
        entityManager.flush();
        Long activityId = activity.getActivityId();
        Long rootId = root.getId();
        entityManager.clear();

        assertThat(comments.findRootPage(activityId, List.of(CommentStatus.PUBLISHED), null, PageRequest.of(0, 20)))
                .extracting(ActivityComment::getId).containsExactly(rootId);
        assertThat(comments.findByActivity_ActivityIdAndParent_IdInAndStatusInOrderByParent_IdAscCreatedAtAsc(
                activityId, List.of(rootId), List.of(CommentStatus.PUBLISHED))).hasSize(1);
        ExternalActivity locked = activities.findByIdForUpdate(activityId).orElseThrow();
        likes.deleteByActivityId(activityId);
        comments.deleteRepliesByActivityId(activityId);
        comments.deleteRootsByActivityId(activityId);
        activities.delete(locked);
        entityManager.flush();
        assertThat(activities.findById(activityId)).isEmpty();
        assertThat(comments.count()).isZero();
        assertThat(likes.count()).isZero();
    }
}
