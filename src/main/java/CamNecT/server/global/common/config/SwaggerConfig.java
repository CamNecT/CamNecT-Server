package CamNecT.server.global.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.auth.SessionId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                version = "v1.0.0"
        )
)
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {
    static {
        // Authentication arguments are injected from the token, never supplied as query parameters.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(UserId.class, SessionId.class);
    }
    @Bean
    public GroupedOpenApi groupedOpenApi() {
        String [] paths = {
                //스웨거 문서에 해당하는 API 설정
                "/**"
        };

        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch(paths)
                .build();
    }
}
