package CamNecT.server.domain.activity;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.activity.model.recruitment.TeamApplication;
import CamNecT.server.domain.activity.model.enums.ApplicationStatus;
import CamNecT.server.domain.activity.service.ActivityService;
import CamNecT.server.domain.activity.service.RecruitmentService;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.report.dto.request.ReportProcessRequest;
import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.model.*;
import CamNecT.server.domain.report.service.ReportService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
    @Autowired ReportService reports;
    @Autowired ActivityService activities;
    @Autowired RecruitmentService recruitments;

    @Test
    void ordinaryActivityDeletionStillHonorsHiddenChildRecruitments() {
        Users owner = createUser(UserRole.USER);
        Users admin = createUser(UserRole.ADMIN);
        var parent = ExternalActivity.builder().user(owner).category(ActivityCategory.EXTERNAL).title("parent").build();
        em.persist(parent);
        var child = TeamRecruitment.builder().activityId(parent.getActivityId()).userId(owner.getUserId())
                .title("child").content("모집").recruitDeadline(LocalDate.now().plusDays(1)).build();
        em.persist(child);
        recruitments.deleteRecruitmentForModeration(admin.getUserId(), child.getRecruitId());
        em.flush();
        em.clear();

        assertThat(em.find(TeamRecruitment.class, child.getRecruitId())).isNull();
        assertThatThrownBy(() -> activities.delete(parent.getActivityId(), owner.getUserId()))
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_HAS_RECRUITMENTS));
    }

    @Test
    void approvedActivityReportHidesListingsAndClosesOnlyPendingApplications() throws Exception {
        Users author = createUser(UserRole.USER);
        Users admin = createUser(UserRole.ADMIN);
        Users applicant = createUser(UserRole.USER);
        Users acceptedMember = createUser(UserRole.USER);
        String title = "moderation-" + UUID.randomUUID();
        ExternalActivity activity = ExternalActivity.builder().user(author).category(ActivityCategory.EXTERNAL)
                .title(title).context("활동").build();
        em.persist(activity);
        TeamRecruitment recruitment = TeamRecruitment.builder().userId(author.getUserId())
                .activityId(activity.getActivityId()).title("모집").content("본문")
                .recruitDeadline(LocalDate.now().plusDays(1)).build();
        em.persist(recruitment);
        TeamApplication pending = application(recruitment, applicant, ApplicationStatus.REQUESTED);
        TeamApplication accepted = application(recruitment, acceptedMember, ApplicationStatus.APPROVED);
        ChatRequest waiting = teamRequest(recruitment, applicant, author);
        ChatRequest joined = teamRequest(recruitment, acceptedMember, author);
        joined.accept();
        ChatRoom room = ChatRoom.createRoom(joined, acceptedMember, author);
        em.persist(room);
        Long submissionId = reports.createReport(applicant.getUserId(), new ReportCreateRequest(author.getUserId(),
                activity.getActivityId(), TargetType.ACTIVITY, ReportCategory.OTHER, "확인 요청", "내용", null));
        Long caseId = em.find(Report.class, submissionId).getReportCase().getCaseId();
        em.flush();
        em.clear();

        reports.processReport(admin.getUserId(), caseId,
                new ReportProcessRequest(ReportStatus.RESOLVED, ReportCategory.OTHER, "승인"));
        em.flush();
        em.clear();

        assertThat(em.find(ReportCase.class, caseId).getStatus()).isEqualTo(ReportStatus.RESOLVED);
        mvc.perform(get("/api/activity").param("title", title)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
        mvc.perform(get("/api/activity/" + activity.getActivityId())).andExpect(status().isNotFound());
        assertThat(em.find(TeamRecruitment.class, recruitment.getRecruitId())).isNull();
        assertThat(em.find(TeamApplication.class, pending.getApplicationId()).getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(em.find(TeamApplication.class, accepted.getApplicationId()).getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(em.find(ChatRequest.class, waiting.getId()).getStatus()).isEqualTo(ChatRequest.RequestStatus.REJECTED);
        assertThat(em.find(ChatRequest.class, joined.getId()).getStatus()).isEqualTo(ChatRequest.RequestStatus.ACCEPTED);
        assertThat(em.find(ChatRoom.class, room.getId()).getStatus()).isEqualTo(ChatRoom.RoomStatus.OPEN);

        String sessionId = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(author.getUserId(), UserRole.USER, sessionId);
        sessions.create(author.getUserId(), access, jwt.generateRefreshToken(author.getUserId(), UserRole.USER, sessionId));
        try {
            String recruitPath = "/api/activity/recruitment/" + recruitment.getRecruitId();
            mvc.perform(get(recruitPath).header("Authorization", "Bearer " + access))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(46403));
            mvc.perform(post(recruitPath + "/apply").header("Authorization", "Bearer " + access)
                            .contentType(APPLICATION_JSON).content("{\"content\":\"지원\"}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(46403));
            mvc.perform(post("/api/request/respond").header("Authorization", "Bearer " + access)
                            .contentType(APPLICATION_JSON)
                            .content("{\"requestId\":" + waiting.getId() + ",\"isAccepted\":true}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(48002));
        } finally {
            sessions.revokeSession(author.getUserId(), sessionId);
        }
    }

    private Users createUser(UserRole role) {
        Users user = Users.builder().username(UUID.randomUUID().toString()).name("회원").role(role)
                .status(UserStatus.ACTIVE).passwordHash("unused").build();
        em.persist(user);
        return user;
    }

    private ChatRequest teamRequest(TeamRecruitment recruitment, Users requester, Users receiver) {
        ChatRequest request = ChatRequest.builder().requester(requester).receiver(receiver)
                .content("지원").type(ChatRequest.RequestType.TEAM_RECRUIT).activityId(recruitment.getActivityId())
                .recruitmentId(recruitment.getRecruitId()).build();
        em.persist(request);
        return request;
    }

    private TeamApplication application(TeamRecruitment recruitment, Users user, ApplicationStatus status) {
        TeamApplication application = TeamApplication.builder().recruitId(recruitment.getRecruitId())
                .userId(user.getUserId()).content("지원 내용").status(status).build();
        em.persist(application);
        return application;
    }

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
        var root = ActivityComment.create(activity, author.getUserId(), null, "댓글");
        em.persist(root);
        em.persist(ActivityComment.create(activity, author.getUserId(), root, "답글"));
        var deleted = ActivityComment.create(activity, author.getUserId(), null, "삭제한 댓글");
        deleted.deleteSoft();
        em.persist(deleted);
        em.flush();

        String sessionId = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(author.getUserId(), UserRole.USER, sessionId);
        sessions.create(author.getUserId(), access, jwt.generateRefreshToken(author.getUserId(), UserRole.USER, sessionId));
        try {
            String activityPath = "/api/activity/" + activity.getActivityId();
            String recruitmentPath = "/api/activity/recruitment/" + recruitment.getRecruitId();
            mvc.perform(get(activityPath)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recruitmentList").isEmpty())
                    .andExpect(jsonPath("$.data.isMine").value(false))
                    .andExpect(jsonPath("$.data.activity.viewCount").value(0))
                    .andExpect(jsonPath("$.data.commentCount").value(2));
            mvc.perform(get(recruitmentPath)).andExpect(status().isUnauthorized());
            for (String path : new String[]{activityPath, recruitmentPath}) {
                mvc.perform(head(path).header("Authorization", "Bearer " + access)).andExpect(status().isOk());
            }
            mvc.perform(get(activityPath).header("Authorization", "Bearer " + access)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.activity.viewCount").value(1))
                    .andExpect(jsonPath("$.data.recruitmentList[0].viewCount").value(0));
            mvc.perform(get(recruitmentPath).header("Authorization", "Bearer " + access)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recruitment.viewCount").value(1))
                    .andExpect(jsonPath("$.data.recruitment.commentCount").value(0));
            mvc.perform(get("/api/activity").param("category", "EXTERNAL").param("title", "활동"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].viewCount").value(1))
                    .andExpect(jsonPath("$.data.content[0].commentCount").value(2));
            em.clear();
            assertThat(em.find(ExternalActivity.class, activity.getActivityId()).getViewCount()).isOne();
            assertThat(em.find(TeamRecruitment.class, recruitment.getRecruitId()).getViewCount()).isOne();
        } finally {
            sessions.revokeSession(author.getUserId(), sessionId);
        }
    }
}
