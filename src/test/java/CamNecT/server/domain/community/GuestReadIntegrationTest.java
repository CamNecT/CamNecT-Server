package CamNecT.server.domain.community;

import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.Posts.*;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.repository.BoardsRepository;
import CamNecT.server.domain.community.service.PostService;
import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestReadIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired BoardsRepository boards;
    @Autowired PostService posts;

    private Users user() {
        var user = Users.builder().username(UUID.randomUUID().toString()).passwordHash("fixture").name("member").build();
        em.persist(user);
        return user;
    }

    private Posts seedPost(BoardCode code, Users owner) {
        var board = boards.findByCode(code).orElseGet(() -> boards.save(Boards.of(code, code.name())));
        var post = Posts.create(board, owner, "title-" + code, "body-" + code, false);
        em.persist(post);
        em.persist(PostStats.init(post));
        em.flush();
        return post;
    }

    @Test
    void guestMayReadInfoButOnlySearchQuestionTitles() throws Exception {
        var owner = user();
        var info = seedPost(BoardCode.INFO, owner);
        var question = seedPost(BoardCode.QUESTION, owner);
        String feed = "/api/community/posts";
        mvc.perform(get(feed + "/" + info.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("body-INFO")).andExpect(jsonPath("$.data.author").isEmpty());
        mvc.perform(get(feed + "/" + info.getId() + "/comments")).andExpect(status().isOk());
        for (String suffix : new String[]{"", "/comments", "/attachments/1/download-url"}) {
            mvc.perform(get(feed + "/" + question.getId() + suffix)).andExpect(status().isUnauthorized());
        }
        for (String sort : new String[]{"LATEST", "LIKE", "BOOKMARK", "RECOMMENDED"}) {
            mvc.perform(get(feed).param("tab","QUESTION").param("keyword","title-QUESTION").param("sort",sort))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].accessStatus").value("LOGIN_REQUIRED"))
                    .andExpect(jsonPath("$.data.items[0].preview").isEmpty()).andExpect(jsonPath("$.data.items[0].thumbnailUrl").isEmpty());
            mvc.perform(get(feed).param("keyword","body-QUESTION").param("sort",sort))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        }
        mvc.perform(get("/api/community/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitingQuestions[0].preview").isEmpty());
        mvc.perform(get("/api/home")).andExpect(status().isOk()).andExpect(jsonPath("$.data.alumni").isEmpty());
        for (String scope : new String[]{"DEFAULT","COMMUNITY_QUESTION","ACTIVITY_RECRUIT"}) {
            mvc.perform(get("/api/tags").param("scope",scope)).andExpect(status().isOk());
        }
        mvc.perform(get(feed).header("Authorization","")).andExpect(status().isUnauthorized());
        mvc.perform(get(feed).header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(post(feed)).andExpect(status().isUnauthorized());
        var reader = user();
        posts.getDetail(owner.getUserId(), info.getId());
        posts.getDetail(reader.getUserId(), info.getId());
        posts.getDetail(reader.getUserId(), info.getId());
        em.flush();
        em.clear();
        assertThat(posts.getDetail(null, info.getId()).viewCount()).isOne();
    }
}
