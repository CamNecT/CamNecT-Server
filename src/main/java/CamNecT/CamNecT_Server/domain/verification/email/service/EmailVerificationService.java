package CamNecT.CamNecT_Server.domain.verification.email.service;

import CamNecT.CamNecT_Server.domain.auth.dto.signup.VerifySignupEmailRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.signup.VerifySignupEmailResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.email.dto.VerifyEmailCodeResponse;
import CamNecT.CamNecT_Server.domain.verification.email.event.EmailVerificationCodeIssuedEvent;
import CamNecT.CamNecT_Server.domain.verification.email.model.EmailTokenUtil;
import CamNecT.CamNecT_Server.domain.verification.email.model.EmailVerificationToken;
import CamNecT.CamNecT_Server.domain.verification.email.repository.EmailVerificationTokenRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import CamNecT.CamNecT_Server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.auth.email-verification.expiration-minutes:30}")
    private long expirationMinutes;


    /**
     * 1차: 이메일만 받고 코드 발급/발송
     * - email 기준으로 미사용 토큰 정리 후 최신 1개만 유지
     */
    @Transactional
    public long sendSignupCode(String email) {
        // 이미 가입된 이메일이면 막기(정책)
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 기존 미사용 코드 정리 (최근 1개만 유효하게 운영)
        tokenRepository.deleteByEmailAndUsedAtIsNull(email);

        String rawCode = EmailTokenUtil.new6DigitCode();
        EmailVerificationToken token = EmailVerificationToken.issueForEmail(email, rawCode, expirationMinutes);
        tokenRepository.save(token);

        applicationEventPublisher.publishEvent(
                new EmailVerificationCodeIssuedEvent(email, rawCode, expirationMinutes)
        );

        return expirationMinutes;
    }

    /**
     * 2차: 코드 검증 성공 시 user 생성 + token.used 처리 + token.user link
     */
    @Transactional
    public VerifySignupEmailResponse verifySignupAndCreateUser(VerifySignupEmailRequest req) {

        // (idempotent) 이미 가입 + 이메일 인증 완료인 경우
        Users existing = userRepository.findByEmail(req.email()).orElse(null);
        if (existing != null && existing.isEmailVerified()) {
            return new VerifySignupEmailResponse(existing.getUserId(), true, null, 0L);
        }

        // 약관 체크
        if (!req.agreements().serviceTerms() || !req.agreements().privacyTerms()) {
            throw new CustomException(AuthErrorCode.TERMS_REQUIRED);
        }

        EmailVerificationToken token = tokenRepository.findTopByEmailAndUsedAtIsNullOrderByIdDesc(req.email())
                .orElseThrow(() -> new CustomException(VerificationErrorCode.NO_ACTIVE_CODE));

        if (token.isExpired()) throw new CustomException(VerificationErrorCode.CODE_EXPIRED_OR_USED);
        if (token.isLocked()) throw new CustomException(VerificationErrorCode.TOO_MANY_ATTEMPTS);

        if (!token.matchesCode(req.code())) {
            token.increaseAttempt();
            if (token.isLocked()) throw new CustomException(VerificationErrorCode.TOO_MANY_ATTEMPTS);
            throw new CustomException(VerificationErrorCode.INVALID_CODE);
        }

        // 비밀번호 정책
        validatePassword(req.password());

        // 최종 유니크 검증(verify 시점이 최종 확정 시점)
        if (userRepository.existsByEmail(req.email())) throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        if (userRepository.existsByUsername(req.username())) throw new CustomException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        if (userRepository.existsByPhoneNum(req.phoneNum())) throw new CustomException(AuthErrorCode.PHONENUM_ALREADY_EXISTS);

        // 유저 생성 (verify 성공 시점에만)
        Users user = Users.builder()
                .email(req.email())
                .username(req.username())
                .name(req.name())
                .phoneNum(req.phoneNum())
                .passwordHash(passwordEncoder.encode(req.password()))
                .termsServiceAgreed(true)
                .termsPrivacyAgreed(true)
                .emailVerified(true)
                .status(UserStatus.ADMIN_PENDING)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // 레이스 컨디션 대비 (DB 유니크가 최종 방어선)
            throw new CustomException(AuthErrorCode.DUPLICATE_RESOURCE);
        }

        // 토큰 사용 처리 + user 연결
        token.markUsed();
        token.linkUser(user);

        // 임시 토큰 발급(기존 로직 유지)
        String tempToken = jwtUtil.generateVerificationToken(user.getUserId(), user.getRole());
        long expiresMinutes = jwtUtil.getVerificationTokenExpirationMs() / 60000L;

        return new VerifySignupEmailResponse(user.getUserId(), false, tempToken, expiresMinutes);
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.{8,16}$)" +                 // 8~16
                    "(?=.*[a-z])" +                   // 소문자 1+
                    "(?=.*\\d)" +                     // 숫자 1+
                    "[A-Za-z\\d!@#$%^&*()_+\\[\\]{}\\\\|;:'\",.<>/?`~=-]+$"
    );

    private void validatePassword(String pw) {
        if (pw == null || !PASSWORD_PATTERN.matcher(pw).matches()) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }
    }
}
