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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestPreviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BoardsRepository boards;

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
                .title("교내 공지").context("공개 본문").build();
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
        mvc.perform(get("/api/preview/activities")).andExpect(status().isOk());
        mvc.perform(get("/api/preview/activities/{id}", activity.getActivityId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("공개 본문"))
                .andExpect(jsonPath("$.data.userId").doesNotExist()).andExpect(jsonPath("$.data.viewCount").doesNotExist());
        mvc.perform(get("/api/preview/alumni/count")).andExpect(status().isOk()).andExpect(jsonPath("$.data.count").isNumber());

        for (String path : new String[]{"/api/community/posts/1", "/api/activity/recruitment/" + recruitment.getRecruitId(),
                "/api/points/transactions", "/api/gifticons/home"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
