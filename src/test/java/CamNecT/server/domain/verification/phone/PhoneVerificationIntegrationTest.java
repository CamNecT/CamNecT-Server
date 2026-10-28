package CamNecT.server.domain.verification.phone;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.jwt.service.TokenSessionService;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.phone-verification.rollout-mode=verified-phone",
        "app.phone-verification.enabled=true",
        "app.phone-verification.access-key=test-access",
        "app.phone-verification.secret-key=test-secret",
        "app.phone-verification.service-id=test-service",
        "app.phone-verification.sender-number=01000000000",
        "app.phone-verification.code-secret=test-code-secret-with-at-least-32-characters",
        "app.phone-verification.cleanup-delay-ms=3600000"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(PhoneVerificationIntegrationTest.TimeConfiguration.class)
class PhoneVerificationIntegrationTest {
    @Autowired PhoneVerificationService service;
    @Autowired UserRepository users;
    @Autowired PhoneChallengeRepository challenges;
    @Autowired SmsQuotaRepository quotas;
    @Autowired PhoneVerificationProperties properties;
    @Autowired PhoneVerificationCleanup cleanup;
    @Autowired TestClock clock;
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired TokenSessionService sessions;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @MockitoBean SmsSender sender;
    private final AtomicReference<String> deliveredCode = new AtomicReference<>();

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary TestClock phoneClock() { return new TestClock(); }
    }

    static class TestClock extends Clock {
        private final AtomicReference<Instant> time = new AtomicReference<>(Instant.parse("2026-10-28T01:00:00Z"));
        void reset() { time.set(Instant.parse("2026-10-28T01:00:00Z")); }
        void advance(long seconds) { time.updateAndGet(value -> value.plusSeconds(seconds)); }
        void set(Instant value) { time.set(value); }
        public ZoneId getZone() { return ZoneId.of("Asia/Seoul"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        public Instant instant() { return time.get(); }
    }

    @BeforeEach
    void setUp() {
        clock.reset();
        properties.setEnabled(true);
        properties.setMonthlyTotal(50);
        properties.setDailyTotal(20);
        properties.setDailyPerPhone(5);
        properties.setDailyPerUser(5);
        challenges.deleteAll();
        quotas.deleteAll();
        quotas.saveAndFlush(new SmsQuota("lock", LocalDateTime.of(9999, 12, 31, 0, 0)));
        when(sender.send(anyString(), anyString())).thenAnswer(call -> {
            deliveredCode.set(call.getArgument(1));
            return "sens-" + UUID.randomUUID();
        });
    }

    @Test
    void verifiesOnceAndKeepsVerificationWhenSameNormalizedNumberIsSaved() {
        Users user = user("01012345678");
        var sent = service.send(user.getUserId());
        String code = deliveredCode.get();
        var stored = challenges.findById(user.getUserId()).orElseThrow();
        assertThat(stored.getCodeHash()).hasSize(64).isNotEqualTo(code);
        assertThat(stored.getPhoneHash()).hasSize(64).doesNotContain("01012345678");
        assertThat(service.verify(user.getUserId(), sent.challengeId(), code).phoneVerified()).isTrue();
        assertThat(challenges.findById(user.getUserId())).isEmpty();
        var updated = service.updatePhone(user.getUserId(), "+82 10-1234-5678");
        assertThat(updated.phoneVerified()).isTrue();
        assertThat(updated.phoneNum()).isEqualTo("01012345678");
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.ALREADY_VERIFIED);
        assertError(() -> service.verify(user.getUserId(), sent.challengeId(), code), PhoneErrorCode.INVALID_CHALLENGE);
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void changingPhoneClearsVerificationAndRequiresNewNumberCode() {
        Users user = user("01012345678");
        var first = service.send(user.getUserId());
        String oldCode = deliveredCode.get();
        service.verify(user.getUserId(), first.challengeId(), oldCode);
        assertThat(service.updatePhone(user.getUserId(), "01099998888").phoneVerified()).isFalse();
        assertError(() -> service.verify(user.getUserId(), first.challengeId(), oldCode), PhoneErrorCode.INVALID_CHALLENGE);
        clock.advance(61);
        var second = service.send(user.getUserId());
        assertThat(service.verify(user.getUserId(), second.challengeId(), deliveredCode.get()).phoneVerified()).isTrue();
        verify(sender).send(eq("01099998888"), anyString());
    }

    @Test
    void pendingCodeCannotVerifyChangedPhoneEvenWhenChangedBack() {
        Users user = user("01012345678");
        var first = service.send(user.getUserId());
        String code = deliveredCode.get();
        service.updatePhone(user.getUserId(), "01099998888");
        service.updatePhone(user.getUserId(), "01012345678");
        assertError(() -> service.verify(user.getUserId(), first.challengeId(), code), PhoneErrorCode.INVALID_CHALLENGE);
        assertThat(service.status(user.getUserId()).phoneVerified()).isFalse();
    }

    @Test
    void failedAttemptsAreCommittedAndCorrectCodeCannotUnlockExhaustedChallenge() {
        Users user = user("01012345678");
        var first = service.send(user.getUserId());
        String code = deliveredCode.get();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 1; i <= 5; i++) {
            assertError(() -> service.verify(user.getUserId(), first.challengeId(), wrong),
                    i == 5 ? PhoneErrorCode.TOO_MANY_ATTEMPTS : PhoneErrorCode.INVALID_CODE);
            assertThat(challenges.findById(user.getUserId()).orElseThrow().getAttempts()).isEqualTo(i);
        }
        assertError(() -> service.verify(user.getUserId(), first.challengeId(), code), PhoneErrorCode.TOO_MANY_ATTEMPTS);
        assertThat(service.status(user.getUserId()).phoneVerified()).isFalse();
    }

    @Test
    void resendInvalidatesPreviousChallengeAndRespectsCooldown() {
        Users user = user("01012345678");
        var old = service.send(user.getUserId());
        String oldCode = deliveredCode.get();
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.RATE_LIMITED);
        clock.advance(61);
        var current = service.send(user.getUserId());
        assertError(() -> service.verify(user.getUserId(), old.challengeId(), oldCode), PhoneErrorCode.INVALID_CHALLENGE);
        assertThat(service.verify(user.getUserId(), current.challengeId(), deliveredCode.get()).phoneVerified()).isTrue();
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(2);
    }

    @Test
    void expiryAndAccountBindingAreEnforced() {
        Users first = user("01012345678");
        Users second = user("01099998888");
        var sent = service.send(first.getUserId());
        String code = deliveredCode.get();
        assertError(() -> service.verify(second.getUserId(), sent.challengeId(), code), PhoneErrorCode.INVALID_CHALLENGE);
        clock.advance(180);
        assertError(() -> service.verify(first.getUserId(), sent.challengeId(), code), PhoneErrorCode.INVALID_CHALLENGE);
        cleanup.cleanup();
        assertThat(challenges.findById(first.getUserId())).isEmpty();
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(1);
    }

    @Test
    void monthlyCapIsSharedAcrossConcurrentAccounts() throws Exception {
        properties.setMonthlyTotal(1);
        Users first = user("01011112222");
        Users second = user("01033334444");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> sendAfter(start, first.getUserId()));
            Future<Boolean> b = pool.submit(() -> sendAfter(start, second.getUserId()));
            start.countDown();
            assertThat(java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(1);
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void sameAccountConcurrentSendOnlyReservesOnce() throws Exception {
        Users user = user("01011112222");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> sendAfter(start, user.getUserId()));
            Future<Boolean> b = pool.submit(() -> sendAfter(start, user.getUserId()));
            start.countDown();
            assertThat(java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void quotaReadSeesCommitMadeAfterAccountSnapshot() {
        Users user = user("01011112222");
        quotas.saveAndFlush(new SmsQuota("month", LocalDateTime.of(2026, 11, 1, 0, 0)));
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            transactions.executeWithoutResult(tx -> {
                // Establish a repeatable-read snapshot without caching the quota entity.
                users.findById(user.getUserId()).orElseThrow();
                try {
                    pool.submit(() -> transactions.executeWithoutResult(other -> {
                        quotas.lockBudget().orElseThrow();
                        quotas.findForUpdate("month").orElseThrow().consume(LocalDateTime.of(2026, 10, 28, 10, 0));
                    })).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                quotas.lockBudget().orElseThrow();
                assertThat(quotas.findForUpdate("month").orElseThrow().getCount()).isEqualTo(1);
            });
        }
    }

    @Test
    void numberChangeDuringProviderCallCannotActivateOldChallenge() {
        Users user = user("01012345678");
        when(sender.send(anyString(), anyString())).thenAnswer(call -> {
            service.updatePhone(user.getUserId(), "01099998888");
            return "accepted";
        });
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.INVALID_CHALLENGE);
        assertThat(challenges.findById(user.getUserId())).isEmpty();
        assertThat(service.status(user.getUserId()).phoneVerified()).isFalse();
        assertThat(service.status(user.getUserId()).phoneNum()).isEqualTo("01099998888");
    }

    @Test
    void providerFailureConsumesBudgetAndCannotAuthenticate() {
        Users user = user("01012345678");
        when(sender.send(anyString(), anyString())).thenThrow(new CustomException(PhoneErrorCode.SMS_SEND_FAILED));
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.SMS_SEND_FAILED);
        var challenge = challenges.findById(user.getUserId()).orElseThrow();
        assertThat(challenge.getSendState()).isEqualTo(PhoneChallenge.SendState.FAILED);
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(1);
        assertError(() -> service.verify(user.getUserId(), challenge.getChallengeId(), "123456"),
                PhoneErrorCode.INVALID_CHALLENGE);
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.RATE_LIMITED);
    }

    @Test
    void disabledSendingFailsClosedBeforeTakingBudget() {
        properties.setEnabled(false);
        Users user = user("01012345678");
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.SMS_UNAVAILABLE);
        assertThat(quotas.findById("month")).isEmpty();
        verifyNoInteractions(sender);
    }

    @Test
    void userQuotaSurvivesPhoneChangesAndNumberQuotaIsSharedAcrossUsers() {
        properties.setDailyPerUser(1);
        properties.setDailyPerPhone(1);
        Users first = user("01012345678");
        service.send(first.getUserId());
        clock.advance(61);
        service.updatePhone(first.getUserId(), "01099998888");
        assertError(() -> service.send(first.getUserId()), PhoneErrorCode.RATE_LIMITED);
        Users second = user("01012345678");
        assertError(() -> service.send(second.getUserId()), PhoneErrorCode.RATE_LIMITED);
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(1);
    }

    @Test
    void cooldownCrossesKoreanMidnightAndMonthlyQuotaDoesNotResetDaily() {
        properties.setDailyPerUser(1);
        clock.set(Instant.parse("2026-10-28T14:59:50Z"));
        Users user = user("01012345678");
        service.send(user.getUserId());
        clock.advance(20);
        assertError(() -> service.send(user.getUserId()), PhoneErrorCode.RATE_LIMITED);
        clock.advance(41);
        service.send(user.getUserId());
        assertThat(quotas.findById("day").orElseThrow().getCount()).isEqualTo(1);
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(2);
    }

    @Test
    void monthResetsAtKoreanMonthBoundary() {
        properties.setMonthlyTotal(1);
        clock.set(Instant.parse("2026-10-31T14:50:00Z"));
        Users user = user("01012345678");
        service.send(user.getUserId());
        clock.advance(601);
        service.send(user.getUserId());
        assertThat(quotas.findById("month").orElseThrow().getCount()).isEqualTo(1);
    }

    @Test
    void missingPhoneAndUnapprovedAccountsCannotSend() {
        Users user = user(null);
        Long id = user.getUserId();
        assertError(() -> service.send(id), PhoneErrorCode.PHONE_REQUIRED);
        user = users.findById(user.getUserId()).orElseThrow();
        user.changePhone("01012345678");
        user.changeStatus(UserStatus.ADMIN_PENDING);
        users.saveAndFlush(user);
        assertThatThrownBy(() -> service.send(id)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.ACTIVE_ACCOUNT_REQUIRED));
        verifyNoInteractions(sender);
    }

    @Test
    void withdrawalClearsStoredNumberAndVerification() {
        Users user = user("01012345678");
        user.verifyPhone(LocalDateTime.now(clock));
        user.withdrawAnonymize("deleted", "deleted-" + user.getUserId(), null, UserStatus.WITHDRAWN);
        users.saveAndFlush(user);
        Users saved = users.findById(user.getUserId()).orElseThrow();
        assertThat(saved.getPhoneNum()).isNull();
        assertThat(saved.getPhoneVerifiedAt()).isNull();
    }

    @Test
    void phoneEndpointsRequireAccessSessionAndRejectSignupTokens() throws Exception {
        mvc.perform(get("/api/profile/me/phone")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile/me/phone").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNum\":\"01012345678\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/profile/me/phone/verification/send")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/profile/me/phone/verification/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isUnauthorized());
        Users user = user("01012345678");
        String token = jwt.generateVerificationToken(user.getUserId(), user.getRole(), user.getPasswordHash());
        mvc.perform(post("/api/profile/me/phone/verification/send").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sender);
    }

    @Test
    void authenticatedHttpFlowReturnsStateAndRejectsMalformedCodeBeforeVerification() throws Exception {
        Users user = user(null);
        String sid = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(user.getUserId(), user.getRole(), sid);
        String refresh = jwt.generateRefreshToken(user.getUserId(), user.getRole(), sid);
        sessions.create(user.getUserId(), access, refresh);
        String header = "Bearer " + access;
        mvc.perform(put("/api/profile/me/phone").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON).content("{\"phoneNum\":\"010-1234-5678\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phoneVerified").value(false));
        String body = mvc.perform(post("/api/profile/me/phone/verification/send").header("Authorization", header))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.expiresInSeconds").value(180))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).path("data").path("challengeId").asText();
        mvc.perform(post("/api/profile/me/phone/verification/confirm").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("challengeId", id, "code", "123"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/me/phone/verification/confirm").header("Authorization", header)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("challengeId", id, "code", deliveredCode.get()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phoneVerified").value(true));
        mvc.perform(get("/api/gifticons/home").header("Authorization", header))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phoneVerified").value(true))
                .andExpect(jsonPath("$.data.phoneNum").value("01012345678"));
    }

    private boolean sendAfter(CountDownLatch start, Long id) throws Exception {
        start.await();
        try { service.send(id); return true; }
        catch (CustomException e) {
            assertThat(e.getErrorCode()).isEqualTo(PhoneErrorCode.RATE_LIMITED);
            return false;
        }
    }

    private Users user(String phone) {
        String suffix = UUID.randomUUID().toString();
        return users.saveAndFlush(Users.builder().username(suffix).email(suffix + "@example.com")
                .name("phone-test").passwordHash("hash").phoneNum(phone).status(UserStatus.ACTIVE).build());
    }

    private void assertError(Runnable action, PhoneErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(error));
    }
}
