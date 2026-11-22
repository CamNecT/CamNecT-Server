package CamNecT.server.global.notification;

import CamNecT.server.domain.users.model.*;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.notification.service.PushDeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PushPreferenceIntegrationTest {
    private static final String URL = "/api/me/notification-preferences";
    @Autowired MockMvc http;
    @Autowired UserRepository users;
    @Autowired TokenSessionService sessions;
    @Autowired JwtUtil jwt;
    @Autowired PushDeviceService devices;

    record Account(long id, String authorization) {}

    private Account account() {
        String sid = UUID.randomUUID().toString();
        long id = users.save(Users.builder().username(sid).email(sid + "@example.invalid")
                .name("push test").passwordHash("fixture").status(UserStatus.ACTIVE).build()).getUserId();
        String token = jwt.generateAccessToken(id, UserRole.USER, sid);
        sessions.create(id, token, jwt.generateRefreshToken(id, UserRole.USER, sid));
        return new Account(id, "Bearer " + token);
    }

    private ResultActions update(Account user, String json) throws Exception {
        return http.perform(patch(URL).header("Authorization", user.authorization())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void register(Account user, String device) throws Exception {
        String request = """
                {"deviceId":"%s","platform":"WEB","token":"%s-%s"}
                """.formatted(device, user.id(), device);
        http.perform(post("/api/notifications/push/tokens").header("Authorization", user.authorization())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
    }

    @Test void masterPreferenceSurvivesRegistrationAndAppliesOnlyToItsOwner() throws Exception {
        var user = account();
        var other = account();
        http.perform(get(URL)).andExpect(status().isUnauthorized());
        http.perform(get(URL).header("Authorization", user.authorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pushEnabled").value(true));
        register(user, "first");
        register(other, "separate");
        update(user, "{\"pushEnabled\":false}").andExpect(status().isOk());
        register(user, "first");
        register(user, "second");
        assertThat(devices.findEnabledTokens(user.id())).isEmpty();
        assertThat(devices.findEnabledTokens(other.id())).hasSize(1);
        update(user, "{}").andExpect(status().isBadRequest());
        update(user, "{\"pushEnabled\":true}").andExpect(status().isOk());
        assertThat(devices.findEnabledTokens(user.id())).hasSize(2);
    }
}
