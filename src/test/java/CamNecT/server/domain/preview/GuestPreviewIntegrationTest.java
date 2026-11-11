package CamNecT.server.domain.preview;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.Posts.Posts;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.model.enums.PostStatus;
import CamNecT.server.domain.community.repository.BoardsRepository;
import CamNecT.server.domain.users.model.*;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.jwt.service.TokenSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestPreviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BoardsRepository boards;
    @Autowired GuestPreviewRepository previews;
    @Autowired JwtUtil jwt;
    @Autowired TokenSessionService sessions;
    @Autowired ObjectMapper objectMapper;

    @Test
    void alumniCountIncludesOnlyActiveProfiles() {
        long before = previews.alumniCount();
        for (UserStatus status : new UserStatus[]{UserStatus.ACTIVE, UserStatus.ADMIN_PENDING, UserStatus.SUSPENDED, UserStatus.WITHDRAWN}) {
            Users user = Users.builder().name("member").username(UUID.randomUUID().toString())
                    .role(UserRole.USER).status(status).passwordHash("unused").build();
            em.persist(user);
            em.persist(UserProfile.builder().user(user).build());
        }
        em.flush();
        assertThat(previews.alumniCount()).isEqualTo(before + 1);
    }

    @Test
    void guestReadsPublishedSummariesAndActivityButCannotReadPrivateResources() throws Exception {
        Users author = Users.builder().name("member").username(UUID.randomUUID().toString())
                .role(UserRole.USER).status(UserStatus.ACTIVE).passwordHash("unused").build();
        em.persist(author);
        em.persist(UserProfile.builder().user(author).build());
        Boards board = boards.findByCode(BoardCode.INFO).orElseGet(() -> boards.save(Boards.of(BoardCode.INFO, "정보")));
        em.persist(Posts.create(board, author, "공개 제목", "회원 전용 본문", false));
        em.persist(Posts.builder().user(author).board(board).title("숨김").content("숨김 본문").status(PostStatus.HIDDEN).build());
        ExternalActivity activity = ExternalActivity.builder().user(author).category(ActivityCategory.EXTERNAL)
                .title("교내 공지").context("회원 전용 본문").thumbnailKey("activity/user-1/thumbnail/example.png").build();
        em.persist(activity);
        TeamRecruitment recruitment = TeamRecruitment.builder().activityId(activity.getActivityId())
                .userId(author.getUserId()).title("모집 제목").content("회원 전용 조건")
                .recruitDeadline(LocalDate.now().plusDays(1)).build();
        em.persist(recruitment);
        em.flush();

        mvc.perform(get("/api/preview/community/posts").param("board", "INFO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("공개 제목"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(0))
                .andExpect(jsonPath("$.data.content[0].commentCount").value(0))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].author").doesNotExist());
        mvc.perform(get("/api/preview/recruitments").param("activityId", activity.getActivityId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("모집 제목"))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
        String body = mvc.perform(get("/api/preview/activities").param("category", "EXTERNAL").param("title", "교내 공지"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode item = objectMapper.readTree(body).path("data").path("content").get(0);
        assertThat(item.path("activityId").asLong()).isEqualTo(activity.getActivityId());
        assertThat(item.path("title").asText()).isEqualTo("교내 공지");
        assertThat(item.path("thumbnailUrl").asText()).isEqualTo("https://cdn.camnect.site/activity/user-1/thumbnail/example.png");
        assertThat(item.properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder("activityId", "title", "thumbnailUrl");
        mvc.perform(get("/api/preview/activities").param("title", "존재하지 않는 제목"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
        mvc.perform(get("/api/preview/activities").param("category", "CLUB").param("title", "교내 공지"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
        mvc.perform(get("/api/preview/activities/{id}", activity.getActivityId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(head("/api/preview/activities/{id}", activity.getActivityId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/activity/{id}", activity.getActivityId()))
                .andExpect(status().isUnauthorized());
        em.flush();
        em.clear();
        assertThat(em.find(ExternalActivity.class, activity.getActivityId()).getViewCount()).isZero();

        String sid = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(author.getUserId(), author.getRole(), sid);
        String refresh = jwt.generateRefreshToken(author.getUserId(), author.getRole(), sid);
        sessions.create(author.getUserId(), access, refresh);
        mvc.perform(get("/api/activity/{id}", activity.getActivityId()).header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activity.context").value("회원 전용 본문"));
        mvc.perform(get("/api/preview/alumni/count")).andExpect(status().isOk()).andExpect(jsonPath("$.data.count").isNumber());

        Users pending = Users.builder().name("pending").username(UUID.randomUUID().toString())
                .role(UserRole.USER).status(UserStatus.ADMIN_PENDING).passwordHash("unused").build();
        em.persist(pending);
        em.persist(UserProfile.builder().user(pending).build());
        em.flush();
        String token = jwt.generateVerificationToken(pending.getUserId(), pending.getRole(), pending.getPasswordHash());
        mvc.perform(get("/api/activity/{id}", activity.getActivityId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/community/posts/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/activity/recruitment/" + recruitment.getRecruitId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        for (String path : new String[]{"/api/community/posts/1", "/api/activity/recruitment/" + recruitment.getRecruitId(),
                "/api/points/transactions", "/api/gifticons/home"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
