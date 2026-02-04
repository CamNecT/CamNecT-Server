package CamNecT.CamNecT_Server.domain.verification.email.service;

import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.email.dto.VerifyEmailCodeResponse;
import CamNecT.CamNecT_Server.domain.verification.email.model.EmailVerificationToken;
import CamNecT.CamNecT_Server.domain.verification.email.repository.EmailVerificationTokenRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.CamNecT_Server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public VerifyEmailCodeResponse verifyEmailCode(Long userId, String rawCode) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.USER_NOT_FOUND));

        String tempToken = jwtUtil.generateAccessToken(user.getUserId(),user.getRole());
        long expiresMinutes = jwtUtil.getVerificationTokenExpirationMs() / 60000L;

        if (user.isEmailVerified()) {
            return new VerifyEmailCodeResponse(user.getUserId(),tempToken, expiresMinutes); // 이미 인증된 경우 idempotent
        }

        EmailVerificationToken token = tokenRepository.findTopByUserAndUsedAtIsNullOrderByIdDesc(user)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.NO_ACTIVE_CODE));

        if (token.isExpired() || token.isUsed()) {
            throw new CustomException(VerificationErrorCode.CODE_EXPIRED_OR_USED);
        }
        if (token.isLocked()) {
            throw new CustomException(VerificationErrorCode.TOO_MANY_ATTEMPTS);
        }

        if (!token.matchesCode(rawCode)) {
            token.increaseAttempt();
            throw new CustomException(VerificationErrorCode.INVALID_CODE);
        }

        token.markUsed();
        user.markEmailVerified();
        user.changeStatus(UserStatus.ADMIN_PENDING);
        return new VerifyEmailCodeResponse(user.getUserId(),tempToken, expiresMinutes);
    }
}
