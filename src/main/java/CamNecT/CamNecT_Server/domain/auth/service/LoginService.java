package CamNecT.CamNecT_Server.domain.auth.service;

import CamNecT.CamNecT_Server.domain.auth.dto.LoginNextStep;
import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginResponse;
import CamNecT.CamNecT_Server.domain.auth.dto.login.VerificationCompleteResponse;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.UserRole;
import CamNecT.CamNecT_Server.domain.users.model.UserStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.document.model.DocumentVerificationSubmission;
import CamNecT.CamNecT_Server.domain.verification.document.repository.DocumentVerificationSubmissionRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.jwt.JwtFacade;
import CamNecT.CamNecT_Server.global.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtFacade jwtFacade;
    private final DocumentVerificationSubmissionRepository submissionRepo;
    private final UserProfileRepository userProfileRepository;
    private final InstitutionRepository institutionRepository;
    private final MajorRepository majorRepository;

    @Transactional
    public LoginResponse login(LoginRequest req) {

        Users user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }

        // 1) 관리자
        if (user.getRole() == UserRole.ADMIN) {
            String access = jwtFacade.createAccessToken(user);
            String refresh = jwtFacade.createRefreshToken(user);

            return new LoginResponse(
                    "Bearer", access, refresh,
                    jwtUtil.getAccessTokenExpirationMs(),
                    jwtUtil.getRefreshTokenExpirationMs(),
                    user.getUserId(),
                    user.getStatus().name(),
                    user.getRole().name(),
                    LoginNextStep.ADMIN_DASHBOARD
            );
        }

        // 2) 최신 증명서 제출 조회
        DocumentVerificationSubmission latest = submissionRepo
                .findTopByUserIdOrderBySubmittedAtDesc(user.getUserId())
                .orElse(null);

        // 3) 온보딩 완료 여부
        boolean onboardingDone = userProfileRepository.existsByUserId(user.getUserId());

        // 4) nextStep 결정
        LoginNextStep nextStep = resolveNext(user, latest, onboardingDone);

        // 5) 토큰 선택
        if (needsVerificationToken(nextStep)) {
            String verification = jwtUtil.generateVerificationToken(user.getUserId(), user.getRole());
            return new LoginResponse(
                    "Bearer", verification, null,
                    jwtUtil.getVerificationTokenExpirationMs(),
                    0L,
                    user.getUserId(),
                    user.getStatus().name(),
                    user.getRole().name(),
                    nextStep
            );
        }

        String access = jwtFacade.createAccessToken(user);
        String refresh = jwtFacade.createRefreshToken(user);

        //TODO : 인증완료화면에서 이름,학과,학번,대학 4가지 return해줘야됨 -> response를 손보기? 첫 로그인한정 api 추가?
        return new LoginResponse(
                "Bearer", access, refresh,
                jwtUtil.getAccessTokenExpirationMs(),
                jwtUtil.getRefreshTokenExpirationMs(),
                user.getUserId(),
                user.getStatus().name(),
                user.getRole().name(),
                nextStep
        );
    }

    public VerificationCompleteResponse getVerificationCompleteInfo(Long userId) {
        Users u = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        UserProfile p = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        String instName = (p.getInstitutionId() == null) ? null
                : institutionRepository.findNameKorById(p.getInstitutionId()).orElse(null);

        String majorName = (p.getMajorId() == null) ? null
                : majorRepository.findNameKorById(p.getMajorId()).orElse(null);

        return new VerificationCompleteResponse(
                u.getName(),
                p.getStudentNo(),
                instName,
                majorName
        );
    }

    private LoginNextStep resolveNext(Users user, DocumentVerificationSubmission latest, boolean onboardingDone) {
        // ACTIVE면 인증완료/홈은 프론트에서 1회 처리(로컬 저장)로 나누는 걸 추천
        if (user.getStatus() == UserStatus.ACTIVE) {
            if (user.isVerificationCompletePending()) {
                user.clearVerificationCompletePending();
                return LoginNextStep.VERIFICATION_COMPLETE;
            }
            return LoginNextStep.HOME;
        }

        // ADMIN_PENDING 흐름
        if (user.getStatus() == UserStatus.ADMIN_PENDING) {
            if (latest == null) return LoginNextStep.DOCUMENT_REQUIRED;

            return switch (latest.getStatus()) {
                case REJECTED, CANCELED -> LoginNextStep.DOCUMENT_REQUIRED;
                case PENDING -> onboardingDone ? LoginNextStep.DOCUMENT_REVIEW_WAITING : LoginNextStep.ONBOARDING_REQUIRED;
                case APPROVED -> LoginNextStep.VERIFICATION_COMPLETE;
            };
        }
        // 방어적 기본값
        return LoginNextStep.HOME;
    }

    private boolean needsVerificationToken(LoginNextStep nextStep) {
        return nextStep == LoginNextStep.ONBOARDING_REQUIRED
                || nextStep == LoginNextStep.DOCUMENT_REQUIRED
                || nextStep == LoginNextStep.DOCUMENT_REVIEW_WAITING;
    }

    public void logout(Long loginUserId) {
        /* stateless access-token only 구조: 서버에서 할 일 없음
                                            (추후 필요하면 푸시토큰 해제, 디바이스 세션 정리 등을 여기서 처리) */}
}