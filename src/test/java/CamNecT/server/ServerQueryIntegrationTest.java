package CamNecT.server;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.chat.repository.ChatRepository;
import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.Comments.CommentLikes;
import CamNecT.server.domain.community.model.Comments.Comments;
import CamNecT.server.domain.community.model.Posts.Posts;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import CamNecT.server.domain.community.repository.Comments.CommentLikesRepository;
import CamNecT.server.domain.community.repository.Comments.CommentsRepository;
import CamNecT.server.domain.community.repository.BoardsRepository;
import CamNecT.server.domain.report.model.ReportCase;
import CamNecT.server.domain.report.model.TargetType;
import CamNecT.server.domain.report.repository.ReportCaseRepository;
import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.EntityManager;
import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServerQueryIntegrationTest {
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired ChatRepository chats;
    @Autowired TeamRecruitmentRepository recruitments;
    @Autowired CommentsRepository comments;
    @Autowired CommentLikesRepository likes;
    @Autowired BoardsRepository boards;
    @Autowired ReportCaseRepository reports;

    @DynamicPropertySource
    static void useDisposableMysqlInCi(DynamicPropertyRegistry properties) {
        String url = System.getenv("QUERY_TEST_MYSQL_URL");
        if (url == null) return;
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.datasource.username", () -> System.getenv("PHONE_MYSQL_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("PHONE_MYSQL_PASSWORD"));
    }

    private Users user(String name) {
        var user = Users.builder().username(UUID.randomUUID().toString()).name(name).passwordHash("fixture").build();
        em.persist(user);
        return user;
    }

    private ChatRoom room(Users sender, Users receiver) {
        var request = ChatRequest.builder().requester(sender).receiver(receiver).requestInterest(List.of())
                .type(ChatRequest.RequestType.COFFEE_CHAT).content("테스트").build();
        em.persist(request);
        var room = ChatRoom.createRoom(request, sender, receiver);
        em.persist(room);
        em.flush();
        return room;
    }

    private void insertMessages(ChatRoom room, Users from, Users to, int count) {
        jdbc.batchUpdate("""
                insert into coffee_chat_message (cc_thread_id, sender_id, receiver_id, content, is_read, created_at, updated_at)
                values (?, ?, ?, ?, false, current_timestamp, current_timestamp)
                """, IntStream.range(0, count).mapToObj(i -> new Object[]{
                        room.getId(), from.getUserId(), to.getUserId(), "message-" + i}).toList());
    }

    private static class Statements extends BaseSessionEventListener {
        int prepared;
        @Override public void jdbcPrepareStatementStart() { prepared++; }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000})
    void readRangeIsConstantCostInStatementsAndExcludesLaterOrOppositeMessages(int size) {
        var sender = user("보낸이");
        var reader = user("읽는이");
        var room = room(sender, reader);
        insertMessages(room, reader, sender, 1);
        insertMessages(room, sender, reader, size);
        Statements statements = new Statements();
        em.unwrap(Session.class).addEventListeners(statements);
        var boundary = chats.findUnreadBoundary(room.getId(), reader.getUserId());
        assertThat(statements.prepared).isEqualTo(1);
        if (size == 0) {
            assertThat(boundary).isEmpty();
            return;
        }
        var edge = boundary.orElseThrow();
        assertThat(edge.getContent()).isEqualTo("message-" + (size - 1));
        insertMessages(room, sender, reader, 1);
        var time = LocalDateTime.of(2026, 1, 1, 8, 0);
        assertThat(chats.markUnreadRange(room.getId(), reader.getUserId(), edge.getFirstId(), edge.getLastId(), time))
                .isEqualTo(size);
        assertThat(statements.prepared).isEqualTo(2);
        assertThat(chats.getReferenceById(edge.getFirstId()).getReadAt()).isEqualTo(time);
        assertThat(chats.getReferenceById(edge.getLastId()).getUpdatedAt()).isEqualTo(time);
        assertThat(chats.markUnreadRange(room.getId(), reader.getUserId(), edge.getFirstId(), edge.getLastId(), time)).isZero();

        // The old room count includes an exited room, while the badge total excludes it.
        room.leave(reader.getUserId());
        em.flush();
        var participants = List.of(sender.getUserId(), reader.getUserId());
        int before = statements.prepared;
        var counts = chats.countForDelivery(room.getId(), participants);
        assertThat(statements.prepared - before).isOne();
        assertThat(counts).hasSize(2);
        for (var row : counts) {
            assertThat(row.getRoomUnread()).isEqualTo(1);
            assertThat(row.getTotalUnread()).isEqualTo(chats.countVisibleUnreadByUserId(row.getUserId()));
        }
    }

    @Test
    void recruitmentAuthorNamesAreLoadedInOneStatement() {
        var author = user("모집 작성자");
        var activity = ExternalActivity.builder().user(author).category(ActivityCategory.EXTERNAL)
                .title("공고").context("내용").build();
        em.persist(activity);
        for (int i = 0; i < 50; i++) {
            em.persist(TeamRecruitment.builder().userId(author.getUserId()).activityId(activity.getActivityId())
                    .title("모집 " + i).content("내용").recruitDeadline(LocalDate.now().plusDays(1)).build());
        }
        em.flush();
        em.clear();
        Statements statements = new Statements();
        em.unwrap(Session.class).addEventListeners(statements);
        var rows = recruitments.findWithAuthorsByActivityId(activity.getActivityId());
        assertThat(rows).hasSize(50).allSatisfy(row ->
                assertThat(row.getRecruitment().toDto(activity.getTitle(), row.getAuthorName()).userName()).isEqualTo("모집 작성자"));
        assertThat(statements.prepared).isOne();
    }

    @Test
    void replyLikesStayWithinVisibleRootsAndCountSoftDeletedReplies() {
        var owner = user("작성자");
        var board = boards.findByCode(BoardCode.INFO).orElseGet(() -> boards.save(Boards.of(BoardCode.INFO, "정보")));
        var post = Posts.create(board, owner, "제목", "내용", false);
        em.persist(post);
        var root = Comments.create(post, owner.getUserId(), null, "루트");
        var otherRoot = Comments.create(post, owner.getUserId(), null, "다른 루트");
        em.persist(root);
        em.persist(otherRoot);
        var reply = Comments.create(post, owner.getUserId(), root, "삭제");
        var hidden = Comments.create(post, owner.getUserId(), root, "숨김");
        reply.deleteSoft();
        hidden.hide();
        em.persist(reply);
        em.persist(hidden);
        for (Comments item : List.of(root, otherRoot, reply, hidden)) em.persist(CommentLikes.of(item, owner.getUserId()));
        em.flush();
        var roots = List.of(root.getId());
        var visible = List.of(CommentStatus.PUBLISHED, CommentStatus.DELETED);
        assertThat(comments.findRepliesInDisplayOrder(post.getId(), roots, visible)).containsExactly(reply);
        assertThat(likes.countForRootPage(post.getId(), roots, visible)).extracting(CommentLikesRepository.LikeCountRow::getCommentId)
                .containsExactlyInAnyOrder(root.getId(), reply.getId());
    }

    @Test
    void reportPageFetchesNamesAndKeepsItsFullCount() {
        for (int i = 0; i < 3; i++) {
            var reported = user("신고 대상 " + i);
            em.persist(ReportCase.open("USER:" + reported.getUserId(), reported, reported.getUserId(), TargetType.USER));
        }
        em.flush();
        em.clear();
        Statements statements = new Statements();
        em.unwrap(Session.class).addEventListeners(statements);
        var page = reports.findAllByFilters(TargetType.USER, null, PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2).allSatisfy(row ->
                assertThat(row.getReportedUser().getName()).startsWith("신고 대상"));
        assertThat(statements.prepared).isEqualTo(2);
    }
}
