package CamNecT.CamNecT_Server.global.common.config;

import CamNecT.CamNecT_Server.global.common.auth.AdminRoleInterceptor;
import CamNecT.CamNecT_Server.global.common.auth.AuthInterceptor;
import CamNecT.CamNecT_Server.global.common.auth.UserIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserIdArgumentResolver userIdArgumentResolver;
    private final AuthInterceptor authInterceptor;
    private final AdminRoleInterceptor adminRoleInterceptor;

    @Value("${security.interceptor.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${security.interceptor.admin.enabled:true}")
    private boolean adminEnabled;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userIdArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (authEnabled) {
            registry.addInterceptor(authInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(
                            "/api/auth/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/api/verification/email/verify-code"
                    );
        }
        if (adminEnabled) {
            registry.addInterceptor(adminRoleInterceptor)
                    .addPathPatterns("/api/admin/**");
        }
    }
}
