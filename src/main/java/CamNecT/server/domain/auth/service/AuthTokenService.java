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

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshAccessToken(String rawRefreshToken) {
        String refreshToken = normalize(rawRefreshToken);
        // 1) 서명/만료 검증
        jwtUtil.validateOrThrow(refreshToken);
        if (jwtUtil.getTokenType(refreshToken) != TokenType.REFRESH) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }

        UserRefreshToken saved = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));

        String incomingHash = TokenUtil.sha256Hex(refreshToken);

        if (saved.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        if (!saved.getRefreshTokenHash().equals(incomingHash)) {
            // 누군가 예전 refresh 재사용(탈취/중복로그인/레이스) 가능
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        String newAccess = jwtUtil.generateAccessToken(userId, user.getRole());
        String newRefresh = jwtUtil.generateRefreshToken(userId, user.getRole());

        // 3) 저장값을 새 refresh로 교체(= 기존 refresh 즉시 무효화)
        String newHash = TokenUtil.sha256Hex(newRefresh);
        LocalDateTime newExp = LocalDateTime.ofInstant(jwtUtil.getExpiration(newRefresh), ZoneId.systemDefault());
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
