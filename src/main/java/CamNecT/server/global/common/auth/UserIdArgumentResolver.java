package CamNecT.server.global.common.auth;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.jwt.model.TokenType;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.jwt.service.TokenSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtUtil jwtUtil;
    private final AccountAccessGuard accountAccessGuard;
    private final TokenSessionService tokenSessionService;
    private final VerificationTokenGuard verificationTokenGuard;

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return (parameter.hasParameterAnnotation(UserId.class)
                && parameter.getParameterType().equals(Long.class))
                || (parameter.hasParameterAnnotation(SessionId.class)
                && parameter.getParameterType().equals(String.class));
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        boolean sessionIdParameter = parameter.hasParameterAnnotation(SessionId.class);
        if (request != null) {
            if (sessionIdParameter && request.getAttribute("sessionId") instanceof String sessionId) {
                return sessionId;
            }
            if (!sessionIdParameter && request.getAttribute("userId") instanceof Long userId) {
                return userId;
            }
        }

        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        String token = extractBearerToken(authHeader);
        TokenType tokenType = validateEndpointTokenType(request, token);
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (CustomException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN, e);
        }
        Users user = accountAccessGuard.requireAccessible(userId);
        String sessionId = null;
        if (tokenType == TokenType.ACCESS) {
            sessionId = tokenSessionService.requireActiveAccess(userId, token);
        } else {
            verificationTokenGuard.requireCurrent(token, user, request);
        }
        if (request != null) {
            request.setAttribute("userId", userId);
            if (sessionId != null) {
                request.setAttribute("sessionId", sessionId);
            }
        }
        if (sessionIdParameter) {
            if (sessionId == null) {
                throw new CustomException(AuthErrorCode.TOKEN_TYPE_NOT_ALLOWED);
            }
            return sessionId;
        }
        return userId;
    }

    private String extractBearerToken(String header) {
        String prefix = "Bearer ";
        if (!header.startsWith(prefix)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }
        return header.substring(prefix.length()).trim();
    }

    private TokenType validateEndpointTokenType(HttpServletRequest request, String token) {
        // Keep JWT expiry/signature errors as 40100, just like AuthInterceptor,
        // so access-token clients can use RTR on /api/auth endpoints as well.
        TokenType tokenType = jwtUtil.getTokenType(token);

        if (tokenType == null) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        boolean allowed = tokenType == TokenType.ACCESS
                || (tokenType == TokenType.VERIFICATION && request != null
                && verificationTokenGuard.isAllowed(request));

        if (!allowed) {
            throw new CustomException(AuthErrorCode.TOKEN_TYPE_NOT_ALLOWED);
        }
        return tokenType;
    }
}
