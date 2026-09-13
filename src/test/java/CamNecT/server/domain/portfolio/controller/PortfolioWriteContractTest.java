package CamNecT.server.domain.portfolio.controller;

import CamNecT.server.domain.community.service.AuthorAssembler;
import CamNecT.server.domain.portfolio.dto.request.PortfolioRequest;
import CamNecT.server.domain.portfolio.model.PortfolioProject;
import CamNecT.server.domain.portfolio.repository.PortfolioAssetRepository;
import CamNecT.server.domain.portfolio.repository.PortfolioRepository;
import CamNecT.server.domain.portfolio.service.PortfolioAttachmentService;
import CamNecT.server.domain.portfolio.service.PortfolioService;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.util.GlobalExceptionHandler;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import CamNecT.server.global.storage.service.PresignEngine;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortfolioWriteContractTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private PortfolioRepository repository;
    private PortfolioProject existing;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(PortfolioRepository.class);
        existing = PortfolioProject.builder()
                .portfolioId(10L).userId(1L).title("before").subtitle("saved subtitle")
                .startDate(LocalDate.of(2026, 8, 1)).build();
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(PortfolioProject.class))).thenAnswer(call -> call.getArgument(0));

        var attachments = mock(PortfolioAttachmentService.class);
        var service = new PortfolioService(
                mock(UserRepository.class), mock(AccountAccessGuard.class), repository,
                mock(PortfolioAssetRepository.class), mock(UploadTicketRepository.class),
                mock(AuthorAssembler.class), mock(PresignEngine.class), mock(PublicUrlIssuer.class), attachments);
        mvc = MockMvcBuilders.standaloneSetup(new PortfolioController(service, attachments))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(UserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return 1L;
                    }
                }).build();
    }

    @ParameterizedTest
    @MethodSource("subtitleUpdates")
    void preservesOmittedSubtitleButHonorsExplicitUpdates(String extraFields, String expected) throws Exception {
        mvc.perform(patch("/api/portfolio/1/10").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectTitle":"after","startedAt":"2026-08-01"%s}
                                """.formatted(extraFields)))
                .andExpect(status().isOk());

        assertThat(existing.getTitle()).isEqualTo("after");
        assertThat(existing.getSubtitle()).isEqualTo(expected);
    }

    private static Stream<Arguments> subtitleUpdates() {
        return Stream.of(
                Arguments.of("", "saved subtitle"),
                Arguments.of(",\"subtitle\":null", null),
                Arguments.of(",\"subtitle\":\"new subtitle\"", "new subtitle"),
                Arguments.of(",\"subtitle\":\"\"", ""),
                Arguments.of(",\"subtitleProvided\":true", "saved subtitle"),
                Arguments.of(",\"subtitle\":null,\"subtitleProvided\":false", null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "update"})
    void acceptsTenFiftyCharacterSkillsWithNoSubtitle(String operation) throws Exception {
        var skills = Collections.nCopies(10, "가".repeat(50));
        var body = validBody();
        body.put("techStack", skills);
        if (operation.equals("create")) body.put("thumbnailKey", "temp/thumbnail.png");
        MockHttpServletRequestBuilder request = operation.equals("create")
                ? post("/api/portfolio/1") : patch("/api/portfolio/1/10");

        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk());

        if (operation.equals("create")) {
            var captor = org.mockito.ArgumentCaptor.forClass(PortfolioProject.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getSubtitle()).isNull();
            assertThat(captor.getValue().getTechStack()).containsExactlyElementsOf(skills);
        } else {
            assertThat(existing.getSubtitle()).isEqualTo("saved subtitle");
            assertThat(existing.getTechStack()).containsExactlyElementsOf(skills);
        }
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidRequestsBeforeWriting(String field, Object value) throws Exception {
        var body = validBody();
        body.put(field, value);
        for (var request : new MockHttpServletRequestBuilder[]{post("/api/portfolio/1"), patch("/api/portfolio/1/10")}) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }
        verifyNoInteractions(repository);
    }

    private static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("techStack", Collections.singletonList("가".repeat(51))),
                Arguments.of("techStack", Collections.nCopies(11, "Java")),
                Arguments.of("techStack", Collections.singletonList(" ")),
                Arguments.of("techStack", Collections.singletonList(null)),
                Arguments.of("subtitle", "가".repeat(51)),
                Arguments.of("projectTitle", " "),
                Arguments.of("startedAt", null),
                Arguments.of("endedAt", "2026-07-31")
        );
    }

    @Test
    void missingAndNullTechStackKeepTheirExistingEmptyListMeaning() throws Exception {
        for (String json : new String[]{"{}", "{\"techStack\":null}"}) {
            assertThat(mapper.readValue(json, PortfolioRequest.class).techStack()).isEmpty();
        }
    }

    @Test
    void openApiDescribesPublicFieldsWithoutExposingPresenceBookkeeping() {
        Schema<?> schema = ModelConverters.getInstance().read(PortfolioRequest.class).get("PortfolioRequest");
        assertThat(schema.getProperties()).containsKeys("projectTitle", "startedAt", "subtitle", "techStack")
                .doesNotContainKeys("subtitleProvided", "dateRangeValid");
        Schema<?> skills = schema.getProperties().get("techStack");
        assertThat(skills.getMaxItems()).isEqualTo(10);
        assertThat(skills.getItems().getMaxLength()).isEqualTo(50);
    }

    private Map<String, Object> validBody() {
        return new LinkedHashMap<>(Map.of("projectTitle", "after", "startedAt", "2026-08-01"));
    }
}
