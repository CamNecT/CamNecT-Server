package CamNecT.server.domain.auth.service;

import CamNecT.server.domain.auth.dto.others.TokenRefreshResponse;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.jwt.model.UserRefreshToken;
import CamNecT.server.global.jwt.repository.UserRefreshTokenRepository;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.jwt.model.TokenType;
import CamNecT.server.global.jwt.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenRefreshResponse refreshAccessToken(String rawRefreshToken) {
        String refreshToken = normalize(rawRefreshToken);
        // 1) 서명/만료 검증
        jwtUtil.validateOrThrow(refreshToken);
        if (jwtUtil.getTokenType(refreshToken) != TokenType.REFRESH) {
            throw new CustomException(AuthErrorCode.TOKEN_TYPE_NOT_ALLOWED);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }

        UserRefreshToken saved = refreshTokenRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));

        String incomingHash = TokenUtil.sha256Hex(refreshToken);

        Instant now = Instant.now();
        if (saved.getExpiresAt().isBefore(now)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!saved.getRefreshTokenHash().equals(incomingHash)) {
            refreshTokenRepository.delete(saved);
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        String newAccess = jwtUtil.generateAccessToken(userId, user.getRole());
        String newRefresh = jwtUtil.generateRefreshToken(userId, user.getRole());

        // 3) 저장값을 새 refresh로 교체(= 기존 refresh 즉시 무효화)
        String newHash = TokenUtil.sha256Hex(newRefresh);
        Instant newExp = jwtUtil.getExpiration(newRefresh); // 이미 Instant로 주는 메서드 있음
        saved.rotate(newHash, newExp);

        return new TokenRefreshResponse(
                "Bearer",
                newAccess,
                jwtUtil.getAccessTokenExpirationMs(),
                newRefresh,
                jwtUtil.getRefreshTokenExpirationMs()
        );
    }

    private String normalize(String token) {
        if (token == null) return null;
        token = token.trim();
        if (token.startsWith("Bearer ")) return token.substring(7).trim();
        return token;
    }
}
