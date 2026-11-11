package CamNecT.server.domain.activity;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.jwt.util.JwtUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityDetailViewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JwtUtil jwt;
    @Autowired TokenSessionService sessions;

    @Test
    void onlySuccessfulDetailGetCountsForActivityAndRecruitment() throws Exception {
        Users author = Users.builder().username(UUID.randomUUID().toString()).name("회원")
                .role(UserRole.USER).status(UserStatus.ACTIVE).passwordHash("unused").build();
        em.persist(author);
        em.persist(UserProfile.builder().user(author).build());
        var activity = ExternalActivity.builder().user(author).category(ActivityCategory.EXTERNAL)
                .title("활동").context("활동 본문").build();
        em.persist(activity);
        var recruitment = TeamRecruitment.builder().userId(author.getUserId()).activityId(activity.getActivityId())
                .title("팀원 모집").content("모집 본문").recruitDeadline(LocalDate.now().plusDays(1)).build();
        em.persist(recruitment);
        em.flush();

        String sessionId = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(author.getUserId(), UserRole.USER, sessionId);
        sessions.create(author.getUserId(), access, jwt.generateRefreshToken(author.getUserId(), UserRole.USER, sessionId));
        try {
            String activityPath = "/api/activity/" + activity.getActivityId();
            String recruitmentPath = "/api/activity/recruitment/" + recruitment.getRecruitId();
            for (String path : new String[]{activityPath, recruitmentPath}) {
                mvc.perform(get(path)).andExpect(status().isUnauthorized());
                mvc.perform(head(path).header("Authorization", "Bearer " + access)).andExpect(status().isOk());
                mvc.perform(get(path).header("Authorization", "Bearer " + access)).andExpect(status().isOk())
                        .andExpect(jsonPath("$..viewCount").isEmpty());
            }
            em.clear();
            assertThat(em.find(ExternalActivity.class, activity.getActivityId()).getViewCount()).isOne();
            assertThat(em.find(TeamRecruitment.class, recruitment.getRecruitId()).getViewCount()).isOne();
        } finally {
            sessions.revokeSession(author.getUserId(), sessionId);
        }
    }
}
