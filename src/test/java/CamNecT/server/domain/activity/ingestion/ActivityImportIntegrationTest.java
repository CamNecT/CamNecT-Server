package CamNecT.server.domain.activity.ingestion;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.activity.service.ActivityService;
import CamNecT.server.domain.users.model.*;
import CamNecT.server.domain.users.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivityImportIntegrationTest {
    private static final String SECRET = "integration-test-only-signing-key-123456789";
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper mapper;
    @Autowired ActivityImportRepository imports;
    @Autowired ExternalActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired ActivityService service;
    @Autowired CamNecT.server.global.jwt.util.JwtUtil jwt;
    @Autowired CamNecT.server.global.jwt.service.TokenSessionService sessions;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean ActivityImportProperties properties;
    @LocalServerPort int port;
    long authorId;

    @BeforeEach void setup() {
        imports.deleteAll();
        activities.deleteAll();
        authorId = new TransactionTemplate(transactionManager).execute(status -> {
            var user = users.save(Users.builder().username("crawler-" + UUID.randomUUID()).name("캠넥트 활동 안내")
                    .passwordHash("not-a-login-credential").role(UserRole.ADMIN).status(UserStatus.ACTIVE).build());
            profiles.save(UserProfile.builder().user(user).build());
            return user.getUserId();
        });
        when(properties.enabled()).thenReturn(true);
        when(properties.sharedSecret()).thenReturn(SECRET);
        when(properties.authorUserId()).thenReturn(authorId);
    }

    private Map<String, Object> payload(long revision) {
        var value = new LinkedHashMap<String, Object>();
        value.put("articleId", "1210911");
        value.put("revision", revision);
        value.put("contentHash", ActivityImportAuth.sha256(("revision" + revision).getBytes(StandardCharsets.UTF_8)));
        value.put("title", "[수영구청] 대학생 경진대회");
        value.put("content", "공모전 안내\n원문: https://job.konkuk.ac.kr/bbs/job/4084/1210911/artclView.do");
        value.put("applyEndDate", "2030-10-30");
        value.put("applyEndAt", "2030-10-30T18:00:00+09:00");
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWQAAAABJRU5ErkJggg==");
        value.put("thumbnail", Map.of("contentType", "image/png", "sha256", ActivityImportAuth.sha256(png),
                "base64", Base64.getEncoder().encodeToString(png)));
        return value;
    }

    private ResponseEntity<JsonNode> post(Map<String, Object> payload) throws Exception {
        return post(payload, SECRET, Instant.now().getEpochSecond());
    }

    private ResponseEntity<JsonNode> readAsUser(String path) {
        String sessionId = UUID.randomUUID().toString();
        String access = jwt.generateAccessToken(authorId, UserRole.ADMIN, sessionId);
        sessions.create(authorId, access, jwt.generateRefreshToken(authorId, UserRole.ADMIN, sessionId));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(access);
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(Map<String, Object> payload, String secret, long timestamp) throws Exception {
        byte[] body = mapper.writeValueAsBytes(payload);
        String message = timestamp + "\nPOST\n" + ActivityImportAuth.PATH + "\n" + ActivityImportAuth.sha256(body);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Crawler-Timestamp", Long.toString(timestamp));
        headers.set("X-Crawler-Signature", HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8))));
        return http.postForEntity(ActivityImportAuth.PATH, new HttpEntity<>(body, headers), JsonNode.class);
    }

    @Test void publishesIntoRealActivityListAndDetailWithThumbnailAndExactDeadline() throws Exception {
        var result = post(payload(1));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().at("/data/status").asText()).isEqualTo("CREATED");
        long id = result.getBody().at("/data/activityId").asLong();
        var listing = service.getActivities(authorId, ActivityCategory.EXTERNAL, null, null, "LATEST", PageRequest.of(0, 10));
        assertThat(listing.getContent()).hasSize(1);
        assertThat(listing.getContent().getFirst().activityId()).isEqualTo(id);
        var detail = service.getActivityDetail(authorId, id);
        assertThat(detail.activity().getApplyEndAt()).isEqualTo(Instant.parse("2030-10-30T09:00:00Z"));
        assertThat(detail.author().userId()).isEqualTo(authorId);
        var response = readAsUser("/api/activity/" + id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().at("/data/activity/thumbnailUrl").asText()).startsWith("https://cdn.camnect.site/");
        String key = activities.findById(id).orElseThrow().getThumbnailKey();
        assertThat(Files.exists(Path.of("build/test-uploads").resolve(key))).isTrue();
        assertThat(post(payload(1)).getBody().at("/data/status").asText()).isEqualTo("UNCHANGED");
        var changed = payload(2);
        changed.put("content", "접수 일정 변경");
        assertThat(post(changed).getBody().at("/data/status").asText()).isEqualTo("UPDATED");
        assertThat(activities.count()).isEqualTo(1);
        assertThat(activities.findById(id).orElseThrow().getContext()).isEqualTo("접수 일정 변경");
        assertThat(activities.findById(id).orElseThrow().getThumbnailKey()).isEqualTo(key);
        assertThat(post(payload(1)).getBody().at("/data/status").asText()).isEqualTo("STALE");
    }

    @Test void rejectsDisabledForgedExpiredAndMalformedUploads() throws Exception {
        assertThat(http.getForEntity("/api/activity", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.getForEntity(ActivityImportAuth.PATH + "/other", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(payload(1), "wrong-secret-key", Instant.now().getEpochSecond()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(payload(1), SECRET, 0).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        when(properties.enabled()).thenReturn(false);
        assertThat(post(payload(1)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        when(properties.enabled()).thenReturn(true);
        var invalid = payload(1);
        invalid.put("thumbnail", Map.of("contentType", "image/png", "sha256", "0".repeat(64), "base64", "AAAA"));
        assertThat(post(invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(activities.count()).isZero();
    }

    @Test void simultaneousRetriesCreateExactlyOneActivity() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> post(payload(1)));
            var second = pool.submit(() -> post(payload(1)));
            assertThat(first.get(30, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.get(30, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(activities.count()).isEqualTo(1);
        assertThat(imports.count()).isEqualTo(1);
    }

    @Test void doesNotOverwriteManualChangesOrRecreateDeletedActivities() throws Exception {
        long id = post(payload(1)).getBody().at("/data/activityId").asLong();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> activities.findById(id).orElseThrow().close());
        assertThat(post(payload(2)).getBody().at("/data/status").asText()).isEqualTo("MANUAL_OVERRIDE");
        activities.deleteById(id);
        assertThat(post(payload(3)).getBody().at("/data/status").asText()).isEqualTo("DELETED");
        assertThat(activities.count()).isZero();
    }

    @Test void missingDatesStillPublishAndExpiredDatesAreClosed() throws Exception {
        var unknown = payload(1);
        unknown.remove("applyEndDate"); unknown.remove("applyEndAt"); unknown.remove("thumbnail");
        long id = post(unknown).getBody().at("/data/activityId").asLong();
        assertThat(activities.findById(id).orElseThrow().getApplyEndDate()).isNull();
        var expired = payload(2);
        expired.put("applyEndDate", "2020-10-30"); expired.put("applyEndAt", "2020-10-30T18:00:00+09:00");
        post(expired);
        assertThat(activities.findById(id).orElseThrow().getStatus().name()).isEqualTo("CLOSED");
    }

    @Test void resolvesConfiguredCamnectUsernameAndNeverPromotesANormalUser() throws Exception {
        var configured = users.save(Users.builder().username("camnect").name("camnect")
                .passwordHash("not-a-login-credential").role(UserRole.USER).status(UserStatus.ACTIVE).build());
        when(properties.authorUserId()).thenReturn(null);
        when(properties.authorUsername()).thenReturn("camnect");
        assertThat(post(payload(1)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(users.findById(configured.getUserId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        users.deleteById(configured.getUserId());
        users.save(Users.builder().username("camnect").name("camnect").passwordHash("not-a-login-credential")
                .role(UserRole.ADMIN).status(UserStatus.ACTIVE).build());
        assertThat(post(payload(1)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "CRAWLER_LIVE_CAPTURE", matches = "true")
    void actualPythonPublisherUploadsAllCapturedArticlesToThisTestServer() throws Exception {
        var builder = new ProcessBuilder("crawlers/konkuk-job/.venv/Scripts/python.exe", "-m", "konkuk_job",
                "--data-dir", "crawlers/konkuk-job/data/smoke", "--publish-only", "--max-articles", "49", "--allow-local-api");
        builder.environment().put("PYTHONPATH", Path.of("crawlers/konkuk-job/src").toAbsolutePath().toString());
        builder.environment().put("CAMNECT_API_BASE_URL", "http://127.0.0.1:" + port);
        builder.environment().put("CAMNECT_IMPORT_SECRET", SECRET);
        builder.redirectErrorStream(true);
        builder.redirectOutput(Path.of("build/crawler-live-publication.log").toFile());
        var process = builder.start();
        assertThat(process.waitFor(180, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).withFailMessage(Files.readString(Path.of("build/crawler-live-publication.log"))).isZero();
        assertThat(imports.count()).isEqualTo(49);
        assertThat(activities.count()).isEqualTo(49);
        assertThat(service.getActivities(authorId, ActivityCategory.EXTERNAL, null, null, "LATEST", PageRequest.of(0, 100)).getContent()).hasSize(49);
        var listed = readAsUser("/api/activity?category=EXTERNAL&size=100");
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody().at("/data/content").size()).isEqualTo(49);
        for (var item : activities.findAll()) {
            assertThat(service.getActivityDetail(authorId, item.getActivityId()).activity().getOfficialUrl()).contains("job.konkuk.ac.kr");
        }
        // A complete second delivery must be idempotent through the real HTTP boundary.
        process = builder.start();
        assertThat(process.waitFor(180, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isZero();
        assertThat(activities.count()).isEqualTo(49);
        assertThat(Files.readString(Path.of("build/crawler-live-publication.log"))).contains("UNCHANGED");
    }
}
