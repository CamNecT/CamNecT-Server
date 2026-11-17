package CamNecT.server.global.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerSuccessResponseAnnotationTest {

    private static final String BASE_PACKAGE = "CamNecT.server";

    @Test
    void everyFrontendRestEndpointDeclaresSwaggerSuccessResponse() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> missingSwagger = new ArrayList<>();
        List<String> missingSuccessResponse = new ArrayList<>();

        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> controller = Class.forName(candidate.getBeanClassName());

            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null) {
                    continue;
                }

                String endpoint = controller.getSimpleName() + "#" + method.getName();
                boolean hasSwagger = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class) != null
                        || AnnotatedElementUtils.findMergedAnnotation(method, ApiResponses.class) != null
                        || method.getAnnotationsByType(ApiResponse.class).length > 0;

                if (!hasSwagger) {
                    missingSwagger.add(endpoint);
                    continue;
                }

                boolean hasSuccessResponse = List.of(method.getAnnotationsByType(ApiResponse.class)).stream()
                        .map(ApiResponse::responseCode)
                        .anyMatch(code -> code.matches("2\\d\\d"));
                if (!hasSuccessResponse) {
                    missingSuccessResponse.add(endpoint);
                }
                var status = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
                String expectedStatus = status == null ? "200" : Integer.toString(status.code().value());
                var successes = List.of(method.getAnnotationsByType(ApiResponse.class)).stream()
                        .filter(response -> response.responseCode().matches("2\\d\\d")).toList();
                assertThat(successes).as(endpoint + " 정상 HTTP 상태").singleElement().satisfies(response -> {
                    assertThat(response.responseCode()).isEqualTo(expectedStatus);
                    if (method.getReturnType() != void.class) {
                        assertThat(response.useReturnTypeSchema()).as(endpoint + " 제네릭 반환 타입 스키마").isTrue();
                    } else {
                        assertThat(response.useReturnTypeSchema()).isFalse();
                        assertThat(response.content()).as(endpoint + " 본문 없는 응답").hasSize(1);
                    }
                });
            }
        }

        missingSwagger.sort(Comparator.naturalOrder());
        missingSuccessResponse.sort(Comparator.naturalOrder());

        assertThat(missingSwagger)
                .as("Swagger annotation이 없는 프론트-서버 REST API")
                .isEmpty();
        assertThat(missingSuccessResponse)
                .as("Swagger 2xx 정상 응답이 없는 프론트-서버 REST API")
                .isEmpty();
    }

}
