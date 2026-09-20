package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.controller.PostController;
import CamNecT.server.domain.community.model.props.CommunityAttachmentProps;
import CamNecT.server.domain.community.repository.Posts.PostAttachmentsRepository;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.config.QuerydslConfig;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.util.GlobalExceptionHandler;
import CamNecT.server.global.storage.config.PresignProps;
import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.storage.model.*;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import java.net.URI;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CommunityUploadReplacementIntegrationTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CommunityUploadReplacementIntegrationTest {
    private static final String ENDPOINT = "/api/community/posts/uploads/presign";
    @Autowired PostAttachmentsService attachments;
    @Autowired PresignEngine engine;
    @Autowired UploadTicketRepository tickets;
    @MockitoBean AccountAccessGuard guard;
    @MockitoBean S3Presigner signer;
    @MockitoBean S3Client s3;
    private MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        var signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://example.com/upload").toURL());
        when(signer.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(mock(PostService.class), mock(PostQueryService.class),
                        mock(PostAttachmentDownloadService.class), attachments))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(UserId.class);
                    }
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) { return 1L; }
                }).build();
    }

    @AfterEach
    void cleanup() { tickets.deleteAll(); }

    @Test
    void retryReplacesOnlyFailedTicketAndKeepsSuccessfulPendingAttachments() throws Exception {
        var failed = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "failed");
        var keep1 = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "keep1");
        var keep2 = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "keep2");
        var used = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.USED, "used");
        var other = ticket(2L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "other");
        var portfolio = ticket(1L, UploadPurpose.PORTFOLIO_ATTACHMENT, UploadTicket.Status.PENDING, "portfolio");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(failed.getStorageKey())))
                .andExpect(status().isOk());
        assertStatus(failed, UploadTicket.Status.EXPIRED);
        for (var kept : List.of(keep1, keep2, other, portfolio)) assertStatus(kept, UploadTicket.Status.PENDING);
        assertStatus(used, UploadTicket.Status.USED);
        assertThat(tickets.countByUserIdAndPurposeAndStatusAndExpiresAtAfter(1L,
                UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, LocalDateTime.now())).isEqualTo(3);
        assertThatThrownBy(() -> engine.consume(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT,
                UploadRefType.POST, 10L, failed.getStorageKey(), "community/posts/post-10/attachments"))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(s3);
    }

    @Test
    void oldClientsStillAppendAndCannotExceedThreePendingFiles() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(null)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(null)))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value(49006));
    }

    @Test
    void limitFailureRollsBackExpiryAndThreeAbandonedTicketsCanAllBeReplaced() throws Exception {
        var first = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "first");
        var second = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "second");
        var third = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "third");
        String one = "{\"contentType\":\"image/png\",\"size\":100,\"originalFilename\":\"new.png\"}";
        String tooMany = body(first.getStorageKey()).replace(one, one + "," + one);
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(tooMany))
                .andExpect(status().isTooManyRequests());
        assertStatus(first, UploadTicket.Status.PENDING);
        String all = body(first.getStorageKey()).replace("\"" + first.getStorageKey() + "\"",
                "\"" + first.getStorageKey() + "\",\"" + second.getStorageKey() + "\",\"" + third.getStorageKey() + "\"");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(all))
                .andExpect(status().isOk());
        for (var old : List.of(first, second, third)) assertStatus(old, UploadTicket.Status.EXPIRED);
    }

    @Test
    void expiredReplacementKeyCanBeRetriedButUsedOtherOwnerOrOtherPurposeCannot() throws Exception {
        var expired = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.EXPIRED, "expired");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(expired.getStorageKey())))
                .andExpect(status().isOk());
        var used = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.USED, "used");
        var other = ticket(2L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "other");
        var portfolio = ticket(1L, UploadPurpose.PORTFOLIO_ATTACHMENT, UploadTicket.Status.PENDING, "portfolio");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(used.getStorageKey())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(49010));
        for (var forbidden : List.of(other, portfolio)) {
            mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(forbidden.getStorageKey())))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(49310));
            assertStatus(forbidden, UploadTicket.Status.PENDING);
        }
        assertStatus(used, UploadTicket.Status.USED);
    }

    @Test
    void invalidInputAndSignerFailurePreserveThePreviousTicket() throws Exception {
        var previous = ticket(1L, UploadPurpose.COMMUNITY_POST_ATTACHMENT, UploadTicket.Status.PENDING, "previous");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(body(previous.getStorageKey()).replace("image/png", "application/x-invalid")))
                .andExpect(status().isUnsupportedMediaType());
        assertStatus(previous, UploadTicket.Status.PENDING);
        when(signer.presignPutObject(any(PutObjectPresignRequest.class))).thenThrow(new IllegalStateException("failed"));
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body(previous.getStorageKey())))
                .andExpect(status().isInternalServerError());
        assertStatus(previous, UploadTicket.Status.PENDING);
        assertThat(tickets.count()).isEqualTo(1);
    }

    private String body(String replaceKey) {
        return """
                {"items":[{"contentType":"image/png","size":100,"originalFilename":"new.png"}]%s}
                """.formatted(replaceKey == null ? "" : ",\"replaceFileKeys\":[\"" + replaceKey + "\"]");
    }
    private UploadTicket ticket(Long userId, UploadPurpose purpose, UploadTicket.Status status, String name) {
        return tickets.saveAndFlush(UploadTicket.builder().userId(userId).purpose(purpose).status(status)
                .storageKey("camnect/temp/" + name + ".png").originalFilename(name + ".png")
                .contentType("image/png").size(100L).expiresAt(LocalDateTime.now().plusMinutes(10)).build());
    }
    private void assertStatus(UploadTicket ticket, UploadTicket.Status status) {
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }
    @TestConfiguration
    static class Config {
        @Bean
        PresignEngine engine(S3Presigner signer, S3Client s3, UploadTicketRepository tickets) {
            return new PresignEngine(signer, s3, new S3Props("test-bucket", "ap-northeast-2", "camnect"),
                    new PresignProps(600, 300), tickets, Clock.systemDefaultZone());
        }
        @Bean
        PostAttachmentsService attachments(PostAttachmentsRepository posts, AccountAccessGuard guard,
                                            PresignEngine engine, UploadTicketRepository tickets, S3Client s3) {
            var methods = new GlobalPresignMethods(s3, new S3Props("test-bucket", "ap-northeast-2", "camnect"), mock(FileStorage.class));
            return new PostAttachmentsService(posts, guard, engine,
                    new CommunityAttachmentProps(3, 10, List.of("image/png", "application/pdf")), methods, tickets);
        }
    }
}
