package CamNecT.server.global.common.config;

import CamNecT.server.domain.activity.ingestion.ActivityImportAuth;
import CamNecT.server.global.common.auth.SessionId;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.exception.InvalidPropertiesException;
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.common.response.InvalidPropertiesErrorResponse;
import CamNecT.server.global.common.response.ValidationErrorResponse;
import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.*;
import CamNecT.server.domain.verification.phone.PhoneErrorCode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Shared responses follow AuthInterceptor/UserIdArgumentResolver and GlobalExceptionHandler. */
@Component
public class ApiResponseDocumentation implements GlobalOperationCustomizer, GlobalOpenApiCustomizer {
    public static final String BEARER_AUTH = "bearerAuth";
    private static final Pattern ERROR_NUMBER = Pattern.compile("(?<!\\d)\\d{5}(?!\\d)");
    private static final Map<Integer, BaseErrorCode> ERRORS = errorCodes();
    private static final Set<String> HMAC_PATHS = Set.of(ActivityImportAuth.PATH, ActivityImportAuth.STUDENT_PATH);

    @Override
    public Operation customize(Operation operation, HandlerMethod handler) {
        if (!handler.getBeanType().getPackageName().startsWith("CamNecT.server")) return operation;
        if (operation.getResponses() == null) operation.setResponses(new ApiResponses());

        String path = path(handler);
        boolean bearer = requiresBearer(handler, path);
        if (bearer) {
            operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
            append(operation, ErrorCode.UNAUTHORIZED, AuthErrorCode.INVALID_TOKEN,
                    AuthErrorCode.ACCESS_TOKEN_REQUIRED, AuthErrorCode.TOKEN_TYPE_NOT_ALLOWED);
            if (path.startsWith("/api/auth/")) append(operation, AuthErrorCode.INVALID_TOKEN_FORMAT);
            append(operation, AuthErrorCode.USER_SUSPENDED, AuthErrorCode.USER_WITHDRAWN);
        }
        if (HMAC_PATHS.contains(path)) {
            operation.addSecurityItem(new SecurityRequirement().addList("crawlerSignature"));
        }
        if (isAdminPath(path)) append(operation, ErrorCode.FORBIDDEN);

        // These responses can occur before controller invocation or in the shared exception handler.
        append(operation, ErrorCode.INTERNAL_ERROR, ErrorCode.NOT_ACCEPTABLE);
        if (bearer || usesPublicTokenStore(path)) append(operation, ErrorCode.REDIS_UNAVAILABLE);
        boolean declaredBody = handler.hasMethodAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class);
        boolean body = declaredBody || Arrays.stream(handler.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(RequestBody.class));
        boolean validation = declaredBody || Arrays.stream(handler.getMethodParameters()).anyMatch(p ->
                Arrays.stream(p.getParameterAnnotations()).anyMatch(a ->
                        a.annotationType().getName().startsWith("jakarta.validation.")));
        if (body) append(operation, ErrorCode.BAD_REQUEST, ErrorCode.UNSUPPORTED_MEDIA_TYPE);

        operation.getResponses().forEach((status, response) -> {
            if (status.matches("2\\d\\d") && response.getContent() != null
                    && response.getContent().containsKey(MediaType.ALL_VALUE)) {
                var json = response.getContent().remove(MediaType.ALL_VALUE);
                response.getContent().putIfAbsent(MediaType.APPLICATION_JSON_VALUE, json);
            }
            if (!status.matches("[45]\\d\\d")) return;
            int httpStatus = Integer.parseInt(status);
            List<Schema<?>> schemas = new ArrayList<>();
            schemas.add(ref(ErrorResponse.class));
            if (httpStatus == 400 && validation) schemas.add(ref(ValidationErrorResponse.class));
            boolean recovery = response.getDescription() != null && response.getDescription().contains("41030");
            if (recovery) schemas.add(ref(InvalidPropertiesErrorResponse.class));
            Schema<?> schema = schemas.getFirst();
            if (schemas.size() > 1) {
                var union = new ComposedSchema();
                schemas.forEach(union::addOneOfItem);
                schema = union;
            }
            var media = new io.swagger.v3.oas.models.media.MediaType().schema(schema);
            for (Integer number : numbers(response.getDescription())) {
                BaseErrorCode error = ERRORS.get(number);
                if (error != null) {
                    media.addExamples(number.toString(), new Example().summary(error.getMessage())
                            .value(new ErrorResponse(error.getHttpStatus().value(), number, error.getMessage())));
                } else if (number == 41030) {
                    var recoveryError = new InvalidPropertiesException(List.of("email"));
                    media.addExamples("41030", new Example().summary("계정 정보 누락·불일치")
                            .value(new InvalidPropertiesErrorResponse(400, 41030, recoveryError.getMessage(), List.of("email"))));
                }
            }
            if (httpStatus == 400 && validation) {
                media.addExamples("40000_validation", new Example().summary("요청 필드 검증 실패")
                        .value(new ValidationErrorResponse(400, 40000, ErrorCode.BAD_REQUEST.getMessage(),
                                List.of(new ValidationErrorResponse.FieldViolation("$request", "요청값이 올바르지 않습니다.")))));
            }
            response.setContent(new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE, media));
            if (httpStatus == 503 && numbers(response.getDescription()).contains(50310)) {
                response.addHeaderObject("Retry-After", new Header().description("Redis 장애 시 재시도까지 대기할 초")
                        .schema(new StringSchema().example("3")));
            }
        });
        return operation;
    }

    @Override
    public void customise(OpenAPI api) {
        if (api.getComponents() == null) api.setComponents(new Components());
        api.getComponents().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("Access Token. 가입용 VERIFICATION 토큰은 허용된 가입·서류 경로에서만 사용합니다."));
        api.getComponents().addSecuritySchemes("crawlerSignature", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Crawler-Signature")
                .description("크롤러 원문 본문·타임스탬프·경로에 대한 HMAC-SHA256 서명(소문자 16진수 64자). Bearer 토큰을 사용하지 않습니다."));
        for (Class<?> type : List.of(ErrorResponse.class, ValidationErrorResponse.class, InvalidPropertiesErrorResponse.class)) {
            ModelConverters.getInstance(api.getOpenapi() != null && api.getOpenapi().startsWith("3.1"))
                    .readAll(type).forEach((name, schema) -> {
                schema.setAdditionalProperties(false);
                if (schema.getProperties() != null) {
                    List<String> required = new ArrayList<>();
                    for (Object property : schema.getProperties().keySet()) required.add((String) property);
                    schema.setRequired(required);
                }
                api.getComponents().addSchemas(name, schema);
            });
        }
    }

    private static void append(Operation operation, BaseErrorCode... errors) {
        for (BaseErrorCode error : errors) {
            String status = Integer.toString(error.getHttpStatus().value());
            ApiResponse response = operation.getResponses().computeIfAbsent(status, ignored -> new ApiResponse());
            if (!numbers(response.getDescription()).contains(error.getCode())) {
                String item = error.getCode() + " " + error.getMessage();
                response.setDescription(response.getDescription() == null || response.getDescription().isBlank()
                        ? item : response.getDescription() + " / " + item);
            }
        }
    }

    private static Set<Integer> numbers(String description) {
        Set<Integer> result = new LinkedHashSet<>();
        if (description != null) ERROR_NUMBER.matcher(description).results()
                .forEach(match -> result.add(Integer.parseInt(match.group())));
        return result;
    }

    private static Schema<?> ref(Class<?> type) {
        return new Schema<>().$ref("#/components/schemas/" + type.getSimpleName());
    }

    private static String path(HandlerMethod handler) {
        RequestMapping controller = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), RequestMapping.class);
        RequestMapping method = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), RequestMapping.class);
        String prefix = controller == null || controller.value().length == 0 ? "" : controller.value()[0];
        String suffix = method == null || method.value().length == 0 ? "" : method.value()[0];
        return (prefix + "/" + suffix).replaceAll("/+", "/");
    }

    private static boolean requiresBearer(HandlerMethod handler, String path) {
        if (Arrays.stream(handler.getMethodParameters()).anyMatch(p ->
                p.hasParameterAnnotation(UserId.class) || p.hasParameterAnnotation(SessionId.class))) return true;
        return path.startsWith("/api/") && !path.startsWith("/api/auth/")
                && !HMAC_PATHS.contains(path);
    }

    private static boolean isAdminPath(String path) {
        var matcher = new AntPathMatcher();
        return Stream.of("/api/admin/**", "/api/activity/admin/**", "/api/v1/reports/admin/**", "/api/notifications/event")
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    private static boolean usesPublicTokenStore(String path) {
        return Set.of("/api/auth/login", "/api/auth/refresh", "/api/auth/password/reset").contains(path);
    }

    private static Map<Integer, BaseErrorCode> errorCodes() {
        Map<Integer, BaseErrorCode> codes = new LinkedHashMap<>();
        Stream.of(ErrorCode.values(), ActivityErrorCode.values(), AuthErrorCode.values(), CoffeeChatErrorCode.values(),
                CommunityErrorCode.values(), GifticonErrorCode.values(), NotificationErrorCode.values(),
                ReportErrorCode.values(), StorageErrorCode.values(), UserErrorCode.values(),
                VerificationErrorCode.values(), PhoneErrorCode.values()).flatMap(Arrays::stream)
                .forEach(error -> codes.put(error.getCode(), error));
        return Collections.unmodifiableMap(codes);
    }
}
