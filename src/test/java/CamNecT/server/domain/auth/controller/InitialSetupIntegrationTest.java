package CamNecT.server.domain.auth.controller;

import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InitialSetupIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void activeUserIsPromptedUntilSetupOrSkipIsExplicitlyCompleted() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "setup-" + suffix;
        String password = "password1";
        Users user = userRepository.saveAndFlush(Users.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .name("setup user")
                .email(username + "@example.com")
                .status(UserStatus.ACTIVE)
                .build());
        userProfileRepository.saveAndFlush(UserProfile.builder().user(user).build());

        String firstLogin = login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextStep").value("VERIFICATION_COMPLETE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode loginResponse = objectMapper.readTree(firstLogin);
        String accessToken = loginResponse.get("accessToken").asText();

        login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextStep").value("VERIFICATION_COMPLETE"));

        mockMvc.perform(post("/api/auth/onboarding")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(userProfileRepository.findByUserId(user.getUserId()).orElseThrow().isInitialSetupCompleted())
                .isTrue();

        login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextStep").value("HOME"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))));
    }

    private byte[] json(Object value) throws Exception {
        return objectMapper.writeValueAsBytes(value);
    }
}
