package CamNecT.server.global.common.auth;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.jwt.model.TokenType;
import CamNecT.server.global.jwt.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtUtil jwtUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        String token = extractBearerToken(authHeader);
        validateAuthEndpointTokenType(webRequest, token);
        try {
            return jwtUtil.getUserId(token);
        } catch (CustomException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN, e);
        }
    }

    private String extractBearerToken(String header) {
        String prefix = "Bearer ";
        if (!header.startsWith(prefix)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }
        return header.substring(prefix.length()).trim();
    }

    private void validateAuthEndpointTokenType(NativeWebRequest webRequest, String token) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null || !request.getRequestURI().startsWith("/api/auth/")) {
            return;
        }

        TokenType tokenType;
        try {
            tokenType = jwtUtil.getTokenType(token);
        } catch (CustomException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN, e);
        }

        if (tokenType == null) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        String uri = request.getRequestURI();
        boolean allowed = switch (uri) {
            case "/api/auth/onboarding" -> tokenType == TokenType.ACCESS || tokenType == TokenType.VERIFICATION;
            case "/api/auth/logout", "/api/auth/verification-complete", "/api/auth/me" -> tokenType == TokenType.ACCESS;
            default -> true;
        };

        if (!allowed) {
            throw new CustomException(AuthErrorCode.TOKEN_TYPE_NOT_ALLOWED);
        }
    }
}
