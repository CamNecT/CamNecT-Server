package CamNecT.server.global.common.auth;

import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.jwt.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shared by the MVC interceptor and the resolver used by /api/auth endpoints. */
@Component
@RequiredArgsConstructor
public class VerificationTokenGuard {

    private final JwtUtil jwtUtil;

    public boolean isAllowed(HttpServletRequest request) {
        if (isOnboardingRequest(request)) return true;

        String uri = request.getRequestURI();
        return switch (request.getMethod()) {
            case "POST" -> uri.equals("/api/verification/documents")
                    || uri.equals("/api/verification/documents/uploads/presign");
            case "GET" -> uri.equals("/api/verification/documents/me")
                    || uri.matches("/api/verification/documents/[0-9]+(?:/download-url)?");
            case "DELETE" -> uri.matches("/api/verification/documents/[0-9]+");
            default -> false;
        };
    }

    public void requireCurrent(String token, Users user, HttpServletRequest request) {
        // Approval may happen while the signup screen is still open. The same
        // token may finish onboarding, but never becomes a regular access token.
        boolean allowedState = user.getStatus() == UserStatus.ADMIN_PENDING
                || (user.getStatus() == UserStatus.ACTIVE && isOnboardingRequest(request));
        if (!isAllowed(request) || !allowedState) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        try {
            String fingerprint = jwtUtil.getPasswordFingerprint(token);
            if (!jwtUtil.matchesPasswordFingerprint(fingerprint, user.getPasswordHash())) {
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }
        } catch (CustomException e) {
            if (e.getErrorCode() == AuthErrorCode.INVALID_TOKEN) throw e;
            throw new CustomException(AuthErrorCode.INVALID_TOKEN, e);
        }
    }

    private boolean isOnboardingRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return ("GET".equals(request.getMethod()) && uri.equals("/api/tags"))
                || ("POST".equals(request.getMethod())
                && (uri.equals("/api/profile/uploads/presign") || uri.equals("/api/auth/onboarding")));
    }
}
