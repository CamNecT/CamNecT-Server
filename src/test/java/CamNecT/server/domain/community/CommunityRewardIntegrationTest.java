package CamNecT.server.domain.community;

import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.Posts.*;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.repository.BoardsRepository;
import CamNecT.server.domain.community.service.PostService;
import CamNecT.server.domain.users.model.*;
import CamNecT.server.global.point.model.PointEvent;
import CamNecT.server.global.point.service.PointService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import CamNecT.server.domain.report.service.UserReportPenaltyService;
import CamNecT.server.domain.users.repository.UserRepository;
import static org.assertj.core.api.Assertions.assertThat;
import CamNecT.server.global.point.service.CommunityRewardService;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(isolation = Isolation.READ_COMMITTED)
class CommunityRewardIntegrationTest {
    @Autowired PostService posts;
    @Autowired PointService points;
    @Autowired CommunityRewardService communityRewards;
    @Autowired BoardsRepository boards;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired UserReportPenaltyService penalties;

    private Users user() {
        var u = Users.builder().username(UUID.randomUUID().toString()).passwordHash("fixture").name("회원")
                .status(UserStatus.ACTIVE).build();
        em.persist(u);
        return u;
    }

    private long post(Users owner) {
        var board = boards.findByCode(BoardCode.INFO).orElseGet(() -> boards.save(Boards.of(BoardCode.INFO, "정보")));
        var post = Posts.create(board, owner, "정보", "내용", false);
        em.persist(post);
        em.persist(PostStats.init(post));
        em.flush();
        return post.getId();
    }

    @Test void allThreeReadersAreReservedAndReplacementsMustQualify() {
        var owner = user();
        var author = owner.getUserId();
        long first = post(owner);
        long second = post(owner);
        var firstReaders = List.of(user(), user(), user());
        for (var reader : firstReaders) posts.toggleLike(reader.getUserId(), first);
        assertThat(points.getBalance(author)).isEqualTo(100);
        assertThat(jdbc.queryForObject("select count(*) from community_reward_pairs where source_type = 'THREELIKES_REWARD' and (high_user_id=? or low_user_id=?)",
                Integer.class, author, author)).isEqualTo(3);
        jdbc.update("update point_transaction set created_at = DATEADD(DAY,-1,CURRENT_TIMESTAMP) where user_id = ?", author);
        for (var reader : firstReaders) posts.toggleLike(reader.getUserId(), second);
        posts.toggleLike(user().getUserId(), second);
        posts.toggleLike(user().getUserId(), second);
        assertThat(points.getBalance(author)).isEqualTo(100);
        posts.toggleLike(user().getUserId(), second);
        assertThat(points.getBalance(author)).isEqualTo(200);
        for (var reader : firstReaders) posts.toggleLike(reader.getUserId(), first);
        assertThat(points.getBalance(author)).isEqualTo(200);
    }

    @Test void dailyQuotaChangesAtSeoulMidnight() {
        var recipient = user();
        var owner = user();
        long recipientId = recipient.getUserId();
        long question = post(owner);
        long midnight = Instant.parse("2026-01-01T15:00:00Z").getEpochSecond();
        var rewards = new CommunityRewardService(points, jdbc, users, penalties,
                Clock.fixed(Instant.ofEpochSecond(midnight + 300), ZoneOffset.UTC));
        var previous = PointEvent.commentSelection(post(owner), 1L);
        points.earnPoint(recipientId, 200, previous);
        points.earnPoint(recipientId, 200, PointEvent.commentSelection(post(owner), 2L));
        jdbc.update("UPDATE point_transaction SET created_at = FROM_UNIXTIME(?) WHERE user_id = ?", midnight, recipientId);
        var selection = PointEvent.commentSelection(question, 3L);
        assertThat(rewards.tryCommunityReward(recipientId, 200, selection, owner.getUserId())).isFalse();
        jdbc.update("UPDATE point_transaction SET created_at = FROM_UNIXTIME(?) WHERE user_id = ? AND event_key = ?",
                midnight - 1, recipientId, previous.eventKey());
        assertThat(rewards.tryCommunityReward(recipientId, 200, selection, owner.getUserId())).isTrue();
    }

    @Test void selectionHonorsBothDirectionsOneGrantPerQuestionAndDailyCaps() {
        var a = user(); var b = user(); var c = user(); var d = user();
        long first = post(a);
        long second = post(b);
        long third = post(c);
        long fourth = post(d);
        long receiver = b.getUserId();
        assertThat(communityRewards.tryCommunityReward(receiver, 200, PointEvent.commentSelection(first, 1L), a.getUserId())).isTrue();
        assertThat(communityRewards.tryCommunityReward(a.getUserId(), 200, PointEvent.commentSelection(second, 2L), receiver)).isFalse();
        assertThat(communityRewards.tryCommunityReward(c.getUserId(), 200, PointEvent.commentSelection(first, 3L), a.getUserId())).isFalse();
        assertThat(communityRewards.tryCommunityReward(receiver, 200, PointEvent.commentSelection(third, 4L), c.getUserId())).isTrue();
        assertThat(communityRewards.tryCommunityReward(receiver, 200, PointEvent.commentSelection(fourth, 5L), d.getUserId())).isFalse();
        assertThat(points.getBalance(receiver)).isEqualTo(400);
    }
}
