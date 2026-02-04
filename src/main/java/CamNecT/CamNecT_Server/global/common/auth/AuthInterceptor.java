package CamNecT.CamNecT_Server.global.common.auth;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.jwt.JwtUtil;
import CamNecT.CamNecT_Server.global.jwt.model.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //제외할 부분은 WebMvcConfig에서 제외함
        // 1. OPTIONS 요청(Preflight)은 통과시킴 (CORS 문제 방지)
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }
        // 토큰 추출
        String token = extractBearer(request);
        //토큰 유효성 검사
        jwtUtil.validateOrThrow(token);

        TokenType type = jwtUtil.getTokenType(token);
        if (type == null) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        String uri = request.getRequestURI();

        if (type == TokenType.REFRESH) { //현재는 refresh는 안쓰고 있습니다!!
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        } else if (type == TokenType.VERIFICATION && !isAllowedForVerificationToken(uri)) {
                throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        Long userId = jwtUtil.getUserId(token);
        request.setAttribute("userId", userId);

        request.setAttribute("role", jwtUtil.getRole(token).name());

        return true;
    }

    //헤더로부터 Bearer 토큰 추출
    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }
        return header.substring(7);
    }

    private boolean isAllowedForVerificationToken(String uri) {
        return uri.startsWith("/api/verification/documents");
    }
}