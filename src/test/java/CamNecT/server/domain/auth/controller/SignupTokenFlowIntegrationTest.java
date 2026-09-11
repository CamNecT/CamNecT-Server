package CamNecT.server.domain.auth.controller;

import CamNecT.server.domain.profile.components.institutions.model.Institutions;
import CamNecT.server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.server.domain.profile.components.majors.model.Majors;
import CamNecT.server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserTagMap;
import CamNecT.server.domain.users.repository.UserTagMapRepository;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.server.domain.verification.document.model.DocumentType;
import CamNecT.server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.server.domain.verification.document.model.VerificationStatus;
import CamNecT.server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.server.domain.verification.document.service.AdminDocumentVerificationService;
import CamNecT.server.domain.verification.document.service.DocumentVerificationService;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.server.global.mail.EmailSender;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.FileStorage;
import CamNecT.server.global.storage.service.GlobalPresignMethods;
import CamNecT.server.global.tag.model.Tag;
import CamNecT.server.global.tag.model.TagCategory;
import CamNecT.server.global.tag.repository.TagRepository;
import CamNecT.server.global.tag.repository.TagCategoryRepository;
import CamNecT.server.global.jwt.model.TokenType;
import CamNecT.server.global.jwt.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
@Import(SignupTokenFlowIntegrationTest.LocalPresignerConfig.class)
class SignupTokenFlowIntegrationTest {

    private static final String SECRET = "test-jwt-secret-key-for-context-loads-at-least-32-characters";
    private static final String PASSWORD = "password1";
    private static final String IMAGE_REQUEST =
            "{\"contentType\":\"image/png\",\"size\":100,\"originalFilename\":\"profile.png\"}";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired DocumentVerificationSubmissionRepository submissions;
    @Autowired InstitutionRepository institutions;
    @Autowired MajorRepository majors;
    @Autowired AdminDocumentVerificationService adminVerification;
    @Autowired DocumentVerificationService documentVerification;
    @Autowired UserTagMapRepository userTags;
    @Autowired TagRepository tags;
    @Autowired TagCategoryRepository tagCategories;
    @Autowired UploadTicketRepository tickets;
    @MockitoBean EmailSender emailSender;
    @MockitoBean S3Client s3;
    @MockitoBean FileStorage fileStorage;
    @MockitoSpyBean GlobalPresignMethods globalPresignMethods;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired PlatformTransactionManager transactionManager;

    @TestConfiguration
    static class LocalPresignerConfig {
        @Bean
        @Primary
        S3Presigner signupTestPresigner() {
            // Signing a URL is local; these dummy credentials never access S3.
            return S3Presigner.builder().region(Region.AP_NORTHEAST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("signup-test-access", "signup-test-secret")))
                    .build();
        }
    }

    @Test
    void pendingSignupCanSkipOnboardingThenApprovalPreservesItAndNotifiesOnce() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        JsonNode firstLogin = login(user, "DOCUMENT_REQUIRED");
        String tempToken = firstLogin.path("accessToken").asText();
        assertThat(jwtUtil.getTokenType(tempToken)).isEqualTo(TokenType.VERIFICATION);
        assertThat(firstLogin.path("refreshToken").isNull()).isTrue();

        DocumentVerificationSubmission submission = submission(user, VerificationStatus.PENDING);
        login(user, "ONBOARDING_REQUIRED");
        signupReads(tempToken);
        onboarding(tempToken, "{}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ADMIN_PENDING"));
        assertThat(profiles.findByUserId(user.getUserId()).orElseThrow().isInitialSetupCompleted()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ADMIN_PENDING);
        login(user, "DOCUMENT_REVIEW_WAITING");

        approve(submission);
        assertThat(users.findById(user.getUserId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(profiles.findByUserId(user.getUserId()).orElseThrow().isInitialSetupCompleted()).isTrue();
        JsonNode approvedLogin = login(user, "VERIFICATION_COMPLETE");
        String accessToken = approvedLogin.path("accessToken").asText();
        assertThat(jwtUtil.getTokenType(accessToken)).isEqualTo(TokenType.ACCESS);
        assertThat(jwtUtil.getTokenType(approvedLogin.path("refreshToken").asText())).isEqualTo(TokenType.REFRESH);

        // The notification was issued by login; the subsequent GET and its retry
        // must remain readable and must never revert onboarding.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/auth/verification-complete").header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("verified user"))
                    .andExpect(jsonPath("$.institutionName").value("Test University"));
        }
        login(user, "HOME");
        login(user, "HOME");
    }

    @Test
    void approvalDuringOnboardingDoesNotInvalidateItsLimitedToken() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        String tempToken = tempToken(user);
        approve(submission(user, VerificationStatus.PENDING));
        JsonNode login = login(user, "ONBOARDING_REQUIRED");
        assertThat(jwtUtil.getTokenType(login.path("accessToken").asText())).isEqualTo(TokenType.ACCESS);
        assertThat(login.path("refreshToken").isTextual()).isTrue();

        signupReads(tempToken);
        onboarding(tempToken, "{\"bio\":\"saved before leaving signup\",\"tagIds\":[]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));
        onboarding(tempToken, "{}").andExpect(status().isCreated());
        assertThat(profiles.findByUserId(user.getUserId()).orElseThrow().getBio())
                .isEqualTo("saved before leaving signup");

        mockMvc.perform(get("/api/verification/documents/me").header("Authorization", bearer(tempToken)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41103));
        mockMvc.perform(get("/api/profile/me").header("Authorization", bearer(tempToken)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        refresh(tempToken).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        login(user, "VERIFICATION_COMPLETE");
        login(user, "HOME");
    }

    @ParameterizedTest
    @EnumSource(value = VerificationStatus.class, names = {"REJECTED", "CANCELED"})
    void rejectedOrCanceledDocumentsTakePriorityOverOnboarding(VerificationStatus state) throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        submission(user, state);
        login(user, "DOCUMENT_REQUIRED");
        profiles.findByUserId(user.getUserId()).orElseThrow().completeInitialSetup();
        login(user, "DOCUMENT_REQUIRED");
    }

    @Test
    void inconsistentApprovedSubmissionNeverGrantsAPendingAccountAnAccessSession() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        submission(user, VerificationStatus.APPROVED);
        JsonNode response = login(user, "ONBOARDING_REQUIRED");
        assertThat(jwtUtil.getTokenType(response.path("accessToken").asText())).isEqualTo(TokenType.VERIFICATION);
        assertThat(response.path("refreshToken").isNull()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"REFRESH", "PASSWORD_RESET"})
    void nonSignupTokensCannotReadTagsUploadOrCompleteOnboarding(TokenType type) throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        String token = type == TokenType.REFRESH
                ? jwtUtil.generateRefreshToken(user.getUserId(), user.getRole(), UUID.randomUUID().toString())
                : jwtUtil.generatePasswordResetToken(user.getUserId(), user.getRole(), user.getPasswordHash());
        mockMvc.perform(get("/api/tags").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        mockMvc.perform(post("/api/profile/uploads/presign").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(IMAGE_REQUEST))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        onboarding(token, "{}").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        assertThat(profiles.findByUserId(user.getUserId()).orElseThrow().isInitialSetupCompleted()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/profile/me", "/api/community/posts", "/api/admin/verification/documents",
            "/api/auth/verification-complete"})
    void tempTokenCannotUseRegularOrAdminApis(String path) throws Exception {
        String token = tempToken(user(UserStatus.ADMIN_PENDING));
        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
    }

    @Test
    void tempTokenCannotLogoutWithdrawOrUseRefresh() throws Exception {
        String token = tempToken(user(UserStatus.ADMIN_PENDING));
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        mockMvc.perform(delete("/api/auth/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
        refresh(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41106));
    }

    @Test
    void passwordChangeRevokesTempTokenOnBothAuthenticationPaths() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        String staleToken = jwtUtil.generateVerificationToken(user.getUserId(), user.getRole(), "old-password-hash");
        mockMvc.perform(get("/api/tags").header("Authorization", bearer(staleToken)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41103));
        onboarding(staleToken, "{}").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41103));
    }

    @Test
    void signupBodyCannotChooseAnotherUsersProfile() throws Exception {
        Users caller = user(UserStatus.ADMIN_PENDING);
        Users other = user(UserStatus.ADMIN_PENDING);
        onboarding(tempToken(caller), "{\"userId\":" + other.getUserId() + ",\"bio\":\"my bio\"}")
                .andExpect(status().isCreated());
        assertThat(profiles.findByUserId(caller.getUserId()).orElseThrow().isInitialSetupCompleted()).isTrue();
        assertThat(profiles.findByUserId(other.getUserId()).orElseThrow().isInitialSetupCompleted()).isFalse();
        assertThat(profiles.findByUserId(other.getUserId()).orElseThrow().getBio()).isNull();
    }

    @Test
    void signupTokenCannotConsumeAnotherUsersImageTicket() throws Exception {
        Users caller = user(UserStatus.ADMIN_PENDING);
        Users other = user(UserStatus.ADMIN_PENDING);
        String upload = mockMvc.perform(post("/api/profile/uploads/presign")
                        .header("Authorization", bearer(tempToken(other)))
                        .contentType(MediaType.APPLICATION_JSON).content(IMAGE_REQUEST))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String key = objectMapper.readTree(upload).path("data").path("fileKey").asText();
        assertThat(key).isNotBlank();
        onboarding(tempToken(caller), objectMapper.writeValueAsString(Map.of("profileImageKey", key)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(49310));
        assertThat(profiles.findByUserId(caller.getUserId()).orElseThrow().isInitialSetupCompleted()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
    void inaccessibleAccountsCannotUseSignupApis(UserStatus state) throws Exception {
        Users user = user(state);
        String token = tempToken(user);
        int code = state == UserStatus.SUSPENDED ? 41302 : 41303;
        mockMvc.perform(get("/api/tags").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(code));
        onboarding(token, "{}").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void expiredAccessTokenCanBeRefreshedOnAuthEndpointsAndRevokedSessionsStayRejected() throws Exception {
        Users user = user(UserStatus.ACTIVE);
        JsonNode login = login(user, "ONBOARDING_REQUIRED");
        String refreshToken = login.path("refreshToken").asText();
        JwtUtil expiredIssuer = new JwtUtil(SECRET, -1L, 60_000L, -1L);
        String expiredAccess = expiredIssuer.generateAccessToken(
                user.getUserId(), user.getRole(), jwtUtil.getSessionId(refreshToken));
        onboarding(expiredAccess, "{}").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(expiredAccess)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40100));
        String expiredTemp = expiredIssuer.generateVerificationToken(user.getUserId(), user.getRole(), user.getPasswordHash());
        onboarding(expiredTemp, "{}").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40100));

        JsonNode rotated = objectMapper.readTree(refresh(refreshToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        String access = rotated.path("accessToken").asText();
        onboarding(access, "{}").andExpect(status().isCreated());
        refresh(refreshToken).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41107));
        onboarding(access, "{}").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(41103));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentLoginsReturnTheCompletionNoticeOnlyOnce() throws Exception {
        Users user = user(UserStatus.ACTIVE);
        UserProfile profile = profiles.findByUserId(user.getUserId()).orElseThrow();
        profile.completeInitialSetup();
        profiles.saveAndFlush(profile);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<String> loginTask = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return login(user).path("nextStep").asText();
            };
            var first = executor.submit(loginTask);
            var second = executor.submit(loginTask);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("VERIFICATION_COMPLETE", "HOME");
        } finally {
            start.countDown();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void committedOnboardingPreservesImageAndTagsAcrossApprovalAndRetries() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        DocumentVerificationSubmission submission = submission(user, VerificationStatus.PENDING);
        String token = tempToken(user);
        Tag tag = tag();
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentType("image/png").contentLength(100L).build());
        String upload = mockMvc.perform(post("/api/profile/uploads/presign")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(IMAGE_REQUEST))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String tempKey = objectMapper.readTree(upload).path("data").path("fileKey").asText();
        String body = objectMapper.writeValueAsString(Map.of(
                "profileImageKey", tempKey, "bio", "persisted bio", "tagIds", java.util.List.of(tag.getId())));

        onboarding(token, body).andExpect(status().isCreated());
        String finalKey = profiles.findByUserId(user.getUserId()).orElseThrow().getProfileImageKey();
        assertThat(finalKey).isNotBlank().isNotEqualTo(tempKey);
        assertThat(tickets.findByStorageKey(finalKey).orElseThrow().getStatus()).isEqualTo(UploadTicket.Status.USED);
        login(user, "DOCUMENT_REVIEW_WAITING");
        approve(submission);
        onboarding(token, body).andExpect(status().isCreated());
        onboarding(token, "{}").andExpect(status().isCreated());
        login(user, "VERIFICATION_COMPLETE");
        login(user, "HOME");

        UserProfile persisted = profiles.findByUserId(user.getUserId()).orElseThrow();
        assertThat(persisted.isInitialSetupCompleted()).isTrue();
        assertThat(persisted.isVerificationCompleteNotified()).isTrue();
        assertThat(persisted.getBio()).isEqualTo("persisted bio");
        assertThat(persisted.getProfileImageKey()).isEqualTo(finalKey);
        assertThat(persisted.getStudentNo()).isEqualTo("2026");
        assertThat(userTags.findAllTagsByUserId(user.getUserId())).extracting(Tag::getId).containsExactly(tag.getId());
        verify(s3, times(1)).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedImageConsumptionRollsBackTagReplacementAndCompletion() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        Tag original = tag();
        Tag replacement = tag();
        userTags.saveAndFlush(UserTagMap.builder().userId(user.getUserId()).tagId(original.getId()).build());
        String body = objectMapper.writeValueAsString(Map.of(
                "profileImageKey", "missing-ticket", "bio", "must roll back",
                "tagIds", java.util.List.of(replacement.getId())));

        onboarding(tempToken(user), body).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(49410));

        UserProfile persisted = profiles.findByUserId(user.getUserId()).orElseThrow();
        assertThat(persisted.isInitialSetupCompleted()).isFalse();
        assertThat(persisted.getBio()).isNull();
        assertThat(userTags.findAllTagsByUserId(user.getUserId())).extracting(Tag::getId).containsExactly(original.getId());
        verifyNoInteractions(s3);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentApprovalAndOnboardingPreserveBothCommittedResults() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        DocumentVerificationSubmission submission = submission(user, VerificationStatus.PENDING);
        String token = tempToken(user);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var onboarding = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return onboarding(token, "{\"bio\":\"concurrent bio\"}").andExpect(status().isCreated());
            });
            var approval = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                approve(submission);
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            onboarding.get(20, TimeUnit.SECONDS);
            approval.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
        }
        UserProfile persisted = profiles.findByUserId(user.getUserId()).orElseThrow();
        assertThat(persisted.isInitialSetupCompleted()).isTrue();
        assertThat(persisted.getBio()).isEqualTo("concurrent bio");
        assertThat(persisted.getStudentNo()).isEqualTo("2026");
        assertThat(users.findById(user.getUserId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        login(user, "VERIFICATION_COMPLETE");
        login(user, "HOME");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancellationAndApprovalCannotBothCommitFromTheSamePendingSubmission() throws Exception {
        Users user = user(UserStatus.ADMIN_PENDING);
        DocumentVerificationSubmission submission = submission(user, VerificationStatus.PENDING);
        CountDownLatch cancellationRead = new CountDownLatch(1);
        CountDownLatch releaseCancellation = new CountDownLatch(1);
        CountDownLatch approvalStarted = new CountDownLatch(1);
        // Pause after the cancellation read/change, before its transaction
        // flushes. Approval must wait rather than consume the same PENDING row.
        doAnswer(invocation -> {
            invocation.callRealMethod();
            cancellationRead.countDown();
            assertThat(releaseCancellation.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(globalPresignMethods).deleteAfterCommit(java.util.Set.of(submission.getStorageKey()));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var cancellation = executor.submit(() -> documentVerification.cancel(user.getUserId(), submission.getId()));
            try {
                assertThat(cancellationRead.await(10, TimeUnit.SECONDS)).isTrue();
                var approval = executor.submit(() -> {
                    approvalStarted.countDown();
                    return assertThrows(CustomException.class, () -> approve(submission));
                });
                assertThat(approvalStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThrows(TimeoutException.class, () -> approval.get(1, TimeUnit.SECONDS));
                releaseCancellation.countDown();
                cancellation.get(20, TimeUnit.SECONDS);
                assertThat(approval.get(20, TimeUnit.SECONDS).getErrorCode())
                        .isEqualTo(VerificationErrorCode.ONLY_PENDING_CAN_REVIEW);
            } finally {
                releaseCancellation.countDown();
            }
        }
        assertThat(submissions.findById(submission.getId()).orElseThrow().getStatus()).isEqualTo(VerificationStatus.CANCELED);
        assertThat(users.findById(user.getUserId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ADMIN_PENDING);
        assertThat(profiles.findByUserId(user.getUserId()).orElseThrow().getStudentNo()).isNull();
        verifyNoInteractions(emailSender);
    }

    @Test
    void cancellationCannotChangeAnotherUsersSubmission() throws Exception {
        Users caller = user(UserStatus.ADMIN_PENDING);
        Users owner = user(UserStatus.ADMIN_PENDING);
        DocumentVerificationSubmission submission = submission(owner, VerificationStatus.PENDING);
        mockMvc.perform(delete("/api/verification/documents/" + submission.getId())
                        .header("Authorization", bearer(tempToken(caller))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(42401));
        assertThat(submission.getStatus()).isEqualTo(VerificationStatus.PENDING);
        verifyNoInteractions(fileStorage);
    }

    private Tag tag() {
        TagCategory category = tagCategories.saveAndFlush(TagCategory.builder()
                .code("signup-" + UUID.randomUUID().toString().substring(0, 8)).name("Signup tags").build());
        return tags.saveAndFlush(Tag.builder().name("Test tag").category(category).build());
    }

    private Users user(UserStatus state) {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            String username = "signup-" + UUID.randomUUID().toString().substring(0, 8);
            Users user = users.saveAndFlush(Users.builder().username(username).name("signup user")
                    .email(username + "@example.com").passwordHash(passwordEncoder.encode(PASSWORD)).status(state).build());
            profiles.saveAndFlush(UserProfile.builder().user(user).build());
            return user;
        });
    }

    private String tempToken(Users user) {
        return jwtUtil.generateVerificationToken(user.getUserId(), user.getRole(), user.getPasswordHash());
    }

    private DocumentVerificationSubmission submission(Users user, VerificationStatus state) {
        return submissions.saveAndFlush(DocumentVerificationSubmission.builder().userId(user.getUserId())
                .docType(DocumentType.ENROLLMENT_CERTIFICATE).status(state).submittedAt(LocalDateTime.now())
                .storageKey("test/document").originalFilename("document.pdf").contentType("application/pdf")
                .size(10L).uploadedAt(LocalDateTime.now()).build());
    }

    private void approve(DocumentVerificationSubmission submission) {
        Institutions institution = institutions.saveAndFlush(Institutions.builder()
                .institutionCode(UUID.randomUUID().toString()).institutionNameKor("Test University").build());
        Majors major = majors.saveAndFlush(Majors.builder().institution(institution)
                .majorCode(UUID.randomUUID().toString()).majorNameKor("Test Major").majorNameEng("Test Major")
                .sortOrder(0).isActive(true).build());
        adminVerification.review(99L, submission.getId(), new AdminReviewDocumentVerificationRequest(
                AdminReviewDocumentVerificationRequest.Decision.APPROVE, null, "verified user", "2026",
                institution.getInstitutionId(), major.getMajorId()));
    }

    private void signupReads(String token) throws Exception {
        mockMvc.perform(get("/api/tags").header("Authorization", bearer(token))).andExpect(status().isOk());
        mockMvc.perform(post("/api/profile/uploads/presign").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(IMAGE_REQUEST)).andExpect(status().isOk());
    }

    private JsonNode login(Users user, String nextStep) throws Exception {
        JsonNode response = login(user);
        assertThat(response.path("nextStep").asText()).isEqualTo(nextStep);
        return response;
    }

    private JsonNode login(Users user) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("username", user.getUsername(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions onboarding(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/auth/onboarding").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", token))));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
