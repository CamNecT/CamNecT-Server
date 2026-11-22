package CamNecT.server.global.openapi;

import CamNecT.server.CamNecTServerApplication;
import CamNecT.server.global.common.auth.SessionId;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.point.model.PointEvent;
import CamNecT.server.global.point.service.PointService;
import jakarta.persistence.EntityManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = CamNecTServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiResponseContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping;
    @Autowired EntityManager em;
    @Autowired JwtUtil jwt;
    @Autowired TokenSessionService sessions;
    @Autowired PointService points;

    @Test
    @Transactional
    void actualSuccessValidationAndDomainErrorsUseTheDocumentedBodies() throws Exception {
        var user = Users.builder().username(UUID.randomUUID().toString()).name("명세 검증 사용자")
                .email(UUID.randomUUID() + "@example.com").passwordHash("unused")
                .role(UserRole.USER).status(UserStatus.ACTIVE).build();
        em.persist(user);
        em.persist(UserProfile.builder().user(user).build());
        em.flush();
        String sid = UUID.randomUUID().toString();
        String token = jwt.generateAccessToken(user.getUserId(), UserRole.USER, sid);
        sessions.create(user.getUserId(), token, jwt.generateRefreshToken(user.getUserId(), UserRole.USER, sid));
        points.earnPoint(user.getUserId(), 2000, PointEvent.signup(user.getUserId()));
        points.spendPoint(user.getUserId(), 1000, PointEvent.gifticonPurchase(user.getUserId(), 123L));

        mvc.perform(get("/api/profile/me/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("명세 검증 사용자"));
        mvc.perform(get("/api/points/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].type").value("SPEND"))
                .andExpect(jsonPath("$.data.items[0].pointChange").value(-1000))
                .andExpect(jsonPath("$.data.items[0].balanceAfter").value(1000))
                .andExpect(jsonPath("$.data.items[1].type").value("EARN"));
        mvc.perform(get("/api/points/transactions").header("Authorization", "Bearer " + token).param("type", "EARN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].pointChange").value(2000));
        mvc.perform(get("/api/points/transactions")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.code").value(41103))
                .andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/points/transactions").header("Authorization", "Bearer " + token).param("size", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.errors").isNotEmpty());
        mvc.perform(get("/api/points/transactions").header("Authorization", "Bearer " + token).param("size", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.errors").doesNotExist());
        mvc.perform(get("/api/gifticons/products/9223372036854775807").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(47401));
        mvc.perform(get("/api/v1/reports/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
        mvc.perform(post("/api/profile/me/phone/verification/send").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(42540));
    }

    @Test
    void bothDocumentsMatchEveryRegisteredEndpoint() throws Exception {
        for (String suffix : new String[]{"", "/default"}) {
            String body = mvc.perform(get("/v3/api-docs" + suffix)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode document = mapper.readTree(body);
            assertThat(document.path("paths").size()).isGreaterThan(100);
            verifyDocument(document);
        }
    }

    private void verifyDocument(JsonNode document) throws Exception {
        Map<Integer, BaseErrorCode> catalog = errorCatalog();
        List<String> problems = new ArrayList<>();
        mapping.getHandlerMethods().forEach((route, handler) -> {
            for (String path : route.getPatternValues()) {
                if (!path.startsWith("/api/")) continue;
                for (var verb : route.getMethodsCondition().getMethods()) {
                    String endpoint = verb + " " + path;
                    JsonNode operation = document.path("paths").path(path).path(verb.name().toLowerCase());
                    if (operation.isMissingNode()) { problems.add(endpoint + " 누락"); continue; }
                    if (operation.path("summary").asText().isBlank()) problems.add(endpoint + " summary 누락");
                    var status = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), ResponseStatus.class);
                    String success = status == null ? "200" : Integer.toString(status.code().value());
                    List<String> actual = new ArrayList<>();
                    operation.path("responses").fieldNames().forEachRemaining(code -> { if (code.startsWith("2")) actual.add(code); });
                    if (!actual.equals(List.of(success))) problems.add(endpoint + " 정상 HTTP 상태 불일치: " + actual);
                    JsonNode content = operation.path("responses").path(success).path("content");
                    if (handler.getReturnType().getParameterType() == void.class) {
                        if (!content.isMissingNode() && !content.isEmpty()) problems.add(endpoint + " void 응답에 본문 존재");
                    } else {
                        JsonNode schema = firstSchema(content);
                        verifyReturnType(document, schema, ResolvableType.forMethodReturnType(handler.getMethod()), endpoint, problems);
                    }
                    Arrays.stream(handler.getMethodParameters()).filter(p -> p.hasParameterAnnotation(UserId.class)
                            || p.hasParameterAnnotation(SessionId.class)).forEach(internal -> {
                        operation.path("parameters").forEach(parameter -> {
                            if (internal.getParameter().getName().equals(parameter.path("name").asText()))
                                problems.add(endpoint + " 내부 인증 파라미터 노출: " + internal.getParameter().getName());
                        });
                    });
                    boolean protectedAuth = Arrays.stream(handler.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(UserId.class))
                            || (!path.startsWith("/api/auth/") && !path.startsWith("/api/internal/activity-imports/"));
                    if (protectedAuth) {
                        if (!operation.path("security").toString().contains("bearerAuth")) problems.add(endpoint + " Bearer 인증 누락");
                        for (String required : List.of("401", "403", "500", "503"))
                            if (!operation.path("responses").has(required)) problems.add(endpoint + " 공통 오류 누락: " + required);
                    }
                    operation.path("responses").fields().forEachRemaining(response -> {
                        if (!response.getKey().matches("[45]\\d\\d")) return;
                        int httpStatus = Integer.parseInt(response.getKey());
                        JsonNode error = response.getValue();
                        JsonNode media = error.path("content").path("application/json");
                        if (media.isMissingNode() || media.path("schema").isMissingNode()) problems.add(endpoint + " 오류 JSON 스키마 누락: " + httpStatus);
                        var numbers = Pattern.compile("(?<!\\d)\\d{5}(?!\\d)").matcher(error.path("description").asText());
                        int count = 0;
                        while (numbers.find()) {
                            count++;
                            int code = Integer.parseInt(numbers.group());
                            BaseErrorCode definition = catalog.get(code);
                            if (definition == null && code != 41030) { problems.add(endpoint + " 존재하지 않는 오류 코드: " + code); continue; }
                            if ((definition == null ? 400 : definition.getHttpStatus().value()) != httpStatus)
                                problems.add(endpoint + " HTTP/오류 코드 불일치: " + httpStatus + "/" + code);
                            JsonNode example = media.path("examples").path(Integer.toString(code)).path("value");
                            if (example.path("status").asInt() != httpStatus || example.path("code").asInt() != code)
                                problems.add(endpoint + " 오류 예시 누락·불일치: " + code);
                            if (definition != null && !definition.getMessage().equals(example.path("message").asText()))
                                problems.add(endpoint + " 오류 예시 메시지 불일치: " + code);
                        }
                        if (count == 0) problems.add(endpoint + " 구체적인 오류 코드 누락: " + httpStatus);
                    });
                }
            }
        });
        verifyRefs(document, document, problems);
        assertThat(problems).as("실제 등록된 모든 API의 Swagger 응답 계약").isEmpty();
        verifySpecificSchemas(document);
    }

    private void verifySpecificSchemas(JsonNode document) {
        JsonNode settings = document.at("/paths/~1api~1profile~1me~1settings/get/responses/200/content");
        assertThat(resolve(document, firstSchema(settings)).path("properties").has("data")).isTrue();
        JsonNode docs = resolve(document, firstSchema(document.at("/paths/~1api~1verification~1documents~1me/get/responses/200/content")));
        assertThat(docs.path("type").asText()).isEqualTo("array");
        JsonNode validation = document.at("/paths/~1api~1points~1transactions/get/responses/400/content/application~1json/schema/oneOf");
        assertThat(validation.toString()).contains("ErrorResponse", "ValidationErrorResponse");
        JsonNode recovery = document.at("/paths/~1api~1auth~1username~1find/post/responses/400/content/application~1json/schema/oneOf");
        assertThat(recovery.toString()).contains("ErrorResponse", "InvalidPropertiesErrorResponse", "ValidationErrorResponse");
        assertThat(document.at("/paths/~1api~1gifticons~1purchases~1confirm/post/responses/403/description").asText()).contains("47301");
        assertThat(document.at("/paths/~1api~1profile~1me~1phone~1verification~1send/post/responses/503/description").asText())
                .contains("42540", "50310");
        assertThat(document.at("/paths/~1api~1points~1transactions/get/responses/503/headers/Retry-After").isMissingNode()).isFalse();
        for (String path : List.of("/api/internal/activity-imports/konkuk-job/tagged", "/api/internal/activity-imports/konkuk-student")) {
            JsonNode operation = document.path("paths").path(path).path("post");
            assertThat(operation.path("security").toString()).contains("crawlerSignature").doesNotContain("bearerAuth");
            assertThat(operation.path("requestBody").path("content").path("application/json").has("schema")).isTrue();
        }
        assertThat(document.at("/paths/~1api~1auth~1login/post/security").isMissingNode()).isTrue();
        assertThat(document.at("/paths/~1api~1auth~1password~1reset/patch/responses/401/description").asText()).contains("41103");
        // Attachment signing is best-effort in these detail services: failures do not become HTTP errors.
        for (String operationPath : List.of("/paths/~1api~1community~1posts~1{postId}/get",
                "/paths/~1api~1portfolio~1{portfolioUserId}~1{portfolioId}/get")) {
            JsonNode operation = document.at(operationPath);
            assertThat(operation.isMissingNode()).isFalse();
            assertThat(operation.path("responses").path("404").path("description").asText()).doesNotContain("49401");
            assertThat(operation.path("responses").path("500").path("description").asText()).doesNotContain("49902");
        }
        for (String name : List.of("ErrorResponse", "ValidationErrorResponse", "InvalidPropertiesErrorResponse")) {
            JsonNode schema = document.path("components").path("schemas").path(name);
            assertThat(schema.path("additionalProperties").asBoolean(true)).isFalse();
            assertThat(schema.path("type").asText()).as(name + " JSON 객체 타입").isEqualTo("object");
        }
        // Distinct nested classes named Item must never overwrite each other's properties or polymorphism.
        JsonNode pointItemRef = document.at("/components/schemas/PointHistoryResponse/properties/items/items");
        assertThat(pointItemRef.path("$ref").asText()).endsWith("/PointHistoryItem");
        JsonNode pointItem = resolve(document, pointItemRef);
        assertThat(pointItem.path("properties").propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("id", "type", "source", "description", "pointChange", "balanceAfter", "createdAt");
        assertThat(pointItem.has("oneOf")).isFalse();
        JsonNode uploadItemRef = document.at("/components/schemas/PresignUploadBatchRequest/properties/items/items");
        assertThat(uploadItemRef.path("$ref").asText()).endsWith("/PresignUploadBatchItem");
        assertThat(resolve(document, uploadItemRef).path("properties").propertyStream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("contentType", "size", "originalFilename");
        JsonNode archiveItem = resolve(document, document.at("/components/schemas/MyArchiveResponse/properties/items/items"));
        assertThat(archiveItem.path("oneOf").size()).isEqualTo(3);
        assertThat(archiveItem.toString()).contains("CommunityItem", "ExternalActivityItem", "RecruitmentItem")
                .doesNotContain("PointHistoryItem", "PresignUploadBatchItem");
        for (var variant : Map.of("CommunityItem", "postId", "ExternalActivityItem", "activityId", "RecruitmentItem", "recruitId").entrySet()) {
            assertThat(document.path("components").path("schemas").path(variant.getKey()).path("required").toString())
                    .contains(variant.getValue());
        }
    }

    private static void verifyReturnType(JsonNode doc, JsonNode schema, ResolvableType type, String where, List<String> problems) {
        JsonNode resolved = resolve(doc, schema);
        Class<?> raw = type.resolve();
        if (raw == ApiResponse.class) {
            if (!resolved.path("properties").has("status") || !resolved.path("properties").has("message")) {
                problems.add(where + " ApiResponse 래퍼 누락"); return;
            }
            if (type.getGeneric(0).resolve() != Void.class)
                verifyReturnType(doc, resolved.path("properties").path("data"), type.getGeneric(0), where + ".data", problems);
        } else if (raw != null && Collection.class.isAssignableFrom(raw)) {
            if (!"array".equals(resolved.path("type").asText()) || !resolved.has("items")) problems.add(where + " 배열 스키마 불일치");
            else verifyReturnType(doc, resolved.path("items"), type.getGeneric(0), where + "[]", problems);
        } else if (raw != null && (Page.class.isAssignableFrom(raw) || Slice.class.isAssignableFrom(raw))) {
            JsonNode content = resolved.path("properties").path("content");
            if (!"array".equals(content.path("type").asText())) problems.add(where + " 페이지 content 배열 누락");
            else verifyReturnType(doc, content.path("items"), type.getGeneric(0), where + ".content[]", problems);
        } else if (resolved.isMissingNode() || resolved.isEmpty()) {
            problems.add(where + " 반환 스키마 누락: " + type);
        } else if (raw != null && raw.getPackageName().startsWith("CamNecT.server")
                && schema.has("$ref") && !(type.hasGenerics()
                ? schema.path("$ref").asText().startsWith("#/components/schemas/" + raw.getSimpleName())
                : schema.path("$ref").asText().endsWith("/" + raw.getSimpleName()))) {
            problems.add(where + " DTO 타입 불일치: " + schema + " / " + raw.getSimpleName());
        }
    }

    private static JsonNode firstSchema(JsonNode content) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(content.elements(), 0), false)
                .findFirst().map(media -> media.path("schema")).orElse(com.fasterxml.jackson.databind.node.MissingNode.getInstance());
    }

    private static JsonNode resolve(JsonNode document, JsonNode node) {
        return node.has("$ref") ? document.at(node.path("$ref").asText().substring(1)) : node;
    }

    private static void verifyRefs(JsonNode doc, JsonNode node, List<String> problems) {
        if (node.isObject() && node.has("$ref")) {
            String ref = node.path("$ref").asText();
            if (ref.startsWith("#/") && doc.at(ref.substring(1)).isMissingNode()) problems.add("깨진 스키마 참조: " + ref);
        }
        if (node.isContainerNode()) node.forEach(child -> verifyRefs(doc, child, problems));
    }

    private static Map<Integer, BaseErrorCode> errorCatalog() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((reader, factory) -> reader.getClassMetadata().isIndependent());
        Map<Integer, BaseErrorCode> codes = new HashMap<>();
        for (var candidate : scanner.findCandidateComponents("CamNecT.server")) {
            Class<?> type = Class.forName(candidate.getBeanClassName(), false, OpenApiResponseContractTest.class.getClassLoader());
            if (type.isEnum() && BaseErrorCode.class.isAssignableFrom(type)) {
                for (Object value : type.getEnumConstants()) {
                    BaseErrorCode error = (BaseErrorCode) value;
                    assertThat(codes.put(error.getCode(), error)).as("오류 코드 중복: " + error.getCode()).isNull();
                }
            }
        }
        return codes;
    }
}
