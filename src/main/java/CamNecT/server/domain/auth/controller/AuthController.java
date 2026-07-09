package CamNecT.server.domain.auth.controller;

import CamNecT.server.domain.auth.dto.login.LoginRequest;
import CamNecT.server.domain.auth.dto.login.LoginResponse;
import CamNecT.server.domain.auth.dto.login.VerificationCompleteResponse;
import CamNecT.server.domain.auth.dto.account.FindUsernameRequest;
import CamNecT.server.domain.auth.dto.account.FindUsernameResponse;
import CamNecT.server.domain.auth.dto.others.TokenRefreshRequest;
import CamNecT.server.domain.auth.dto.others.TokenRefreshResponse;
import CamNecT.server.domain.auth.dto.others.WithdrawRequest;
import CamNecT.server.domain.auth.dto.password.ResetPasswordRequest;
import CamNecT.server.domain.auth.dto.password.SendPasswordResetEmailRequest;
import CamNecT.server.domain.auth.dto.password.SendPasswordResetEmailResponse;
import CamNecT.server.domain.auth.dto.signup.*;
import CamNecT.server.domain.auth.dto.signup.SendSignupEmailRequest;
import CamNecT.server.domain.auth.dto.signup.SendSignupEmailResponse;
import CamNecT.server.domain.auth.dto.signup.VerifySignupEmailRequest;
import CamNecT.server.domain.auth.dto.signup.VerifySignupEmailResponse;
import CamNecT.server.domain.auth.service.AccountRecoveryService;
import CamNecT.server.domain.auth.service.AuthTokenService;
import CamNecT.server.domain.auth.service.LoginService;
import CamNecT.server.domain.profile.dto.request.UpdateOnboardingRequest;
import CamNecT.server.domain.profile.dto.response.ProfileStatusResponse;
import CamNecT.server.domain.profile.service.ProfileService;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.verification.email.service.EmailVerificationService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가: 로그인, 회원가입")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final ProfileService profileService;
    private final AuthTokenService authTokenService;
    private final AccountRecoveryService accountRecoveryService;

    @Operation(
            summary = "로그인",
            description = "아이디/비밀번호로 로그인하고 토큰을 발급합니다."
    )
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest req) {
        return loginService.login(req);
    }



    @Operation(summary = "아이디 중복확인", description = "사용 가능하면 true, 이미 사용 중이면 false")
    @GetMapping("/{username}/available")
    public boolean isUsernameAvailable(@PathVariable String username) {
        String u = (username == null) ? "" : username.trim();
        if (u.isBlank()) return false;          // 빈 값은 사용 불가
        return !userRepository.existsByUsername(u);
    }

    /**
     * 1차) 이메일 발송
     * POST /api/auth/signup/email/send
     */
    @Operation(
            summary = "회원가입 이메일 인증코드 발송",
            description = "입력한 이메일로 6자리 인증코드를 발송합니다. (아직 유저는 생성하지 않습니다.)"
    )
    @PostMapping("/signup/email/send")
    @ResponseStatus(HttpStatus.OK)
    public SendSignupEmailResponse sendSignupEmail(@RequestBody @Valid SendSignupEmailRequest req) {
        long expiresMinutes = emailVerificationService.sendSignupCode(req.email());
        return new SendSignupEmailResponse(req.email(), expiresMinutes);
    }

    /**
     * 2차) 이메일 인증 + 회원가입 정보 확정(유저 생성)
     * POST /api/auth/signup/email/verify
     */
    @Operation(
            summary = "회원가입 이메일 인증 + 유저 생성",
            description = "이메일 인증코드를 검증한 뒤, 회원 정보를 확정하여 유저를 생성합니다."
    )
    @PostMapping("/signup/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public VerifySignupEmailResponse verifySignupEmail(@RequestBody @Valid VerifySignupEmailRequest req) {
        return emailVerificationService.verifySignupAndCreateUser(req);
    }


    @Operation(
            summary = "온보딩 정보 등록",
            description = "회원가입 후 초기 프로필 설정(온보딩) 정보를 저장합니다."
    )
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileStatusResponse createOnboarding(
            @UserId Long userId,
            @RequestBody @Valid UpdateOnboardingRequest req
    ) {
        return profileService.createOnboarding(userId, req);
    }


    @Operation(
            summary = "로그아웃",
            description = "클라이언트에서 access token을 삭제합니다. 서버는 별도 세션/리프레시 토큰을 관리하지 않으므로 200 OK만 반환합니다."
    )
    @PostMapping("/logout")
    public void logout(@UserId Long loginUserId) {
        loginService.logout(loginUserId);
    }

    @Operation(summary = "인증 완료 화면 정보 조회", description = "인증 완료 화면에 필요한 이름/학번/학교/학과를 반환합니다.")
    @GetMapping("/verification-complete")
    public VerificationCompleteResponse verificationComplete(@UserId Long userId) {
        return loginService.getVerificationCompleteInfo(userId);
    }

    @Operation(summary = "AccessToken 재발급", description = "유효한 refreshToken으로 새 accessToken을 발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest req) {
        return ApiResponse.success(authTokenService.refreshAccessToken(req.refreshToken()));
    }

    @Operation(summary = "아이디 찾기", description = "이름과 이메일을 확인하여 가입된 아이디를 조회합니다.")
    @PostMapping("/username/find")
    @ResponseStatus(HttpStatus.OK)
    public FindUsernameResponse findUsername(@RequestBody FindUsernameRequest req) {
        return accountRecoveryService.findUsername(req);
    }

    @Operation(summary = "비밀번호 재설정 이메일 코드 전송", description = "가입된 이메일로 비밀번호 재설정용 6자리 인증 코드를 전송합니다.")
    @PostMapping("/password/reset/email/send")
    @ResponseStatus(HttpStatus.OK)
    public SendPasswordResetEmailResponse sendPasswordResetEmail(@RequestBody SendPasswordResetEmailRequest req) {
        long expiresMinutes = emailVerificationService.sendPasswordResetCode(req.username(), req.email());
        return new SendPasswordResetEmailResponse(req.email(), expiresMinutes);
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일 인증 코드를 검증한 뒤 비밀번호를 변경합니다.")
    @PatchMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        emailVerificationService.verifyPasswordResetAndUpdatePassword(req);
    }

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 탈퇴 처리(익명화)합니다.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @UserId Long userId,
            @RequestBody @Valid WithdrawRequest req
    ) {
        loginService.withdraw(userId, req);
    }
}
