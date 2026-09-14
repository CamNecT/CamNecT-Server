package CamNecT.server.domain.portfolio.controller;

import CamNecT.server.domain.portfolio.model.props.PortfolioAssetProps;
import CamNecT.server.domain.portfolio.model.props.PortfolioThumbnailProps;
import CamNecT.server.domain.portfolio.service.PortfolioAttachmentService;
import CamNecT.server.domain.portfolio.service.PortfolioService;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.config.QuerydslConfig;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.server.global.common.util.GlobalExceptionHandler;
import CamNecT.server.global.storage.config.PresignProps;
import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.storage.model.UploadPurpose;
import CamNecT.server.global.storage.model.UploadRefType;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.FileStorage;
import CamNecT.server.global.storage.service.GlobalPresignMethods;
import CamNecT.server.global.storage.service.PresignEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, PortfolioThumbnailUploadIntegrationTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PortfolioThumbnailUploadIntegrationTest {
    private static final String ENDPOINT = "/api/portfolio/1/uploads/presign/thumbnail";
    private static final String UPLOAD_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/thumbnail.png";

    @Autowired PortfolioAttachmentService attachments;
    @Autowired PresignEngine engine;
    @Autowired UploadTicketRepository tickets;
    @MockitoBean AccountAccessGuard accessGuard;
    @MockitoBean S3Presigner signer;
    @MockitoBean S3Client s3;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        var signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(URI.create(UPLOAD_URL).toURL());
        when(signer.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        mvc = MockMvcBuilders.standaloneSetup(new PortfolioController(mock(PortfolioService.class), attachments))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(UserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) {
                        return 1L;
                    }
                }).build();
    }

    @AfterEach
    void cleanUp() {
        tickets.deleteAll();
    }

    @Test
    void retryAndImageReplacementSucceedBeforeThePreviousTicketExpires() throws Exception {
        for (String filename : List.of("first.png", "first.png", "replacement.png")) {
            mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                            .content(body("image/png", 100, filename)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.uploadUrl").value(UPLOAD_URL))
                    .andExpect(jsonPath("$.data.fileKey").isNotEmpty())
                    .andExpect(jsonPath("$.data.requiredHeaders['Content-Type']").value("image/png"));

            assertThat(activeThumbnails()).hasSize(1)
                    .allSatisfy(ticket -> assertThat(ticket.getOriginalFilename()).isEqualTo(filename));
        }
        assertThat(tickets.findAll()).filteredOn(t -> t.getStatus() == UploadTicket.Status.EXPIRED).hasSize(2);
        verify(accessGuard, times(3)).requireAccessibleForUpdate(1L);
        verifyNoInteractions(s3);
    }

    @Test
    void replacementDoesNotInvalidateUsedTicketsOtherUsersOrOtherUploadPurposes() throws Exception {
        var abandoned = saveTicket(1L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "old");
        var used = saveTicket(1L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.USED, "used");
        var otherUser = saveTicket(2L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "other-user");
        var asset = saveTicket(1L, UploadPurpose.PORTFOLIO_ATTACHMENT, UploadTicket.Status.PENDING, "asset");
        var profile = saveTicket(1L, UploadPurpose.PROFILE_IMAGE, UploadTicket.Status.PENDING, "profile");

        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(body("image/png", 100, "new.png")))
                .andExpect(status().isOk());

        assertStatus(abandoned, UploadTicket.Status.EXPIRED);
        assertStatus(used, UploadTicket.Status.USED);
        assertStatus(otherUser, UploadTicket.Status.PENDING);
        assertStatus(asset, UploadTicket.Status.PENDING);
        assertStatus(profile, UploadTicket.Status.PENDING);
        assertThat(activeThumbnails()).hasSize(1);
    }

    @Test
    void supersededThumbnailCannotBeConsumedEvenBeforeItsOriginalExpiration() throws Exception {
        var previous = saveTicket(1L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "previous");
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(body("image/png", 100, "new.png")))
                .andExpect(status().isOk());

        assertThatThrownBy(() -> engine.consume(1L, UploadPurpose.PORTFOLIO_THUMBNAIL,
                UploadRefType.PORTFOLIO, 10L, previous.getStorageKey(), "portfolio/user-1/portfolio-10/thumbnail"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StorageErrorCode.UPLOAD_TICKET_EXPIRED_OR_USED));
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @CsvSource({"application/pdf,100,415,49004", "image/png,10485761,413,49005", "image/png,0,400,49020"})
    void invalidReplacementKeepsThePreviousTicket(String contentType, long size, int status, int code) throws Exception {
        var previous = saveTicket(1L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "previous");

        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(body(contentType, size, "invalid")))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));

        assertStatus(previous, UploadTicket.Status.PENDING);
        assertThat(tickets.count()).isEqualTo(1);
        verifyNoInteractions(signer, s3);
    }

    @Test
    void presigningFailureRollsBackTheReplacement() throws Exception {
        var previous = saveTicket(1L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "previous");
        when(signer.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(new IllegalStateException("presigning unavailable"));

        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(body("image/png", 100, "new.png")))
                .andExpect(status().isInternalServerError());

        assertStatus(previous, UploadTicket.Status.PENDING);
        assertThat(tickets.count()).isEqualTo(1);
    }

    @Test
    void anotherUserCannotReplaceTheOwnersPendingThumbnail() throws Exception {
        var previous = saveTicket(2L, UploadPurpose.PORTFOLIO_THUMBNAIL, UploadTicket.Status.PENDING, "owner");
        mvc.perform(post("/api/portfolio/2/uploads/presign/thumbnail").contentType(MediaType.APPLICATION_JSON)
                        .content(body("image/png", 100, "new.png")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(44310));
        assertStatus(previous, UploadTicket.Status.PENDING);
        verifyNoInteractions(accessGuard, signer, s3);
    }

    @Test
    void openingThePresignEndpointWithGetReturns405WithoutIssuingATicket() throws Exception {
        mvc.perform(get(ENDPOINT).param("userId", "1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40500));
        assertThat(tickets.count()).isZero();
        verifyNoInteractions(accessGuard, signer, s3);
    }

    private List<UploadTicket> activeThumbnails() {
        return tickets.findAll().stream()
                .filter(t -> t.getUserId().equals(1L) && t.getPurpose() == UploadPurpose.PORTFOLIO_THUMBNAIL)
                .filter(t -> t.isUsable(LocalDateTime.now())).toList();
    }

    private UploadTicket saveTicket(Long userId, UploadPurpose purpose, UploadTicket.Status status, String name) {
        return tickets.saveAndFlush(UploadTicket.builder().userId(userId).purpose(purpose).status(status)
                .storageKey("camnect/temp/" + name + ".png").originalFilename(name + ".png")
                .contentType("image/png").size(100L).expiresAt(LocalDateTime.now().plusMinutes(10)).build());
    }

    private void assertStatus(UploadTicket ticket, UploadTicket.Status expected) {
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(expected);
    }

    private static String body(String contentType, long size, String filename) {
        return """
                {"contentType":"%s","size":%d,"originalFilename":"%s"}
                """.formatted(contentType, size, filename);
    }

    @TestConfiguration
    static class Config {
        @Bean
        PresignEngine presignEngine(S3Presigner signer, S3Client s3, UploadTicketRepository tickets) {
            return new PresignEngine(signer, s3, new S3Props("test-bucket", "ap-northeast-2", "camnect"),
                    new PresignProps(600, 300), tickets, Clock.systemDefaultZone());
        }

        @Bean
        PortfolioAttachmentService attachments(AccountAccessGuard guard, PresignEngine engine,
                                                UploadTicketRepository tickets, S3Client s3) {
            var methods = new GlobalPresignMethods(s3, new S3Props("test-bucket", "ap-northeast-2", "camnect"),
                    mock(FileStorage.class));
            return new PortfolioAttachmentService(guard, engine, tickets, methods,
                    new PortfolioAssetProps(10, 10, List.of("image/png", "application/pdf")),
                    new PortfolioThumbnailProps(10));
        }
    }
}
