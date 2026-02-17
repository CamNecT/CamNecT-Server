package CamNecT.server.global.common.auth;

import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.jwt.util.JwtUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AdminRoleInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // 1) OPTIONS 요청(Preflight)은 통과 (CORS)
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, new IllegalArgumentException("Authorization 헤더가 존재하지 않습니다."));
        }

        String token = extractBearerToken(authHeader);

        UserRole role = jwtUtil.getRole(token);
        if (role != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN, new IllegalArgumentException("관리자 권한이 필요합니다."));
        }
        return true;
    }

    private String extractBearerToken(String header) {
        String prefix = "Bearer ";
        if (!header.startsWith(prefix)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, new IllegalArgumentException("Authorization 헤더 형식이 올바르지 않습니다."));
        }
        return header.substring(prefix.length()).trim();
    }
}