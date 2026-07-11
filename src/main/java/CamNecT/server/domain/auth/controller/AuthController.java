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
import CamNecT.server.domain.auth.dto.password.VerifyPasswordResetEmailRequest;
import CamNecT.server.domain.auth.dto.password.VerifyPasswordResetEmailResponse;
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
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.common.response.InvalidPropertiesErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패 또는 잘못된 JSON",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "정지 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "아이디에 해당하는 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 인증번호 전송 성공",
                    content = @Content(schema = @Schema(implementation = SendSignupEmailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패 또는 잘못된 JSON",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공 및 유저 생성 성공",
                    content = @Content(schema = @Schema(implementation = VerifySignupEmailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패, 인증번호 없음/만료/불일치, 약관 미동의, 비밀번호 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이메일/아이디/전화번호 중복 또는 리소스 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "인증번호 시도 횟수 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/signup/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public VerifySignupEmailResponse verifySignupEmail(@RequestBody @Valid VerifySignupEmailRequest req) {
        return emailVerificationService.verifySignupAndCreateUser(req);
    }


    @Operation(
            summary = "온보딩 정보 등록",
            description = "회원가입 후 초기 프로필 설정(온보딩) 정보를 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "온보딩 정보 등록 성공",
                    content = @Content(schema = @Schema(implementation = ProfileStatusResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패, 잘못된 태그 ID, 업로드 티켓 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 또는 유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "정지 사용자, 이메일 미인증 또는 업로드 티켓 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자/프로필/업로드 티켓/스토리지 파일 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "스토리지 파일 이동/조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 또는 유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/logout")
    public void logout(@UserId Long loginUserId) {
        loginService.logout(loginUserId);
    }

    @Operation(summary = "인증 완료 화면 정보 조회", description = "인증 완료 화면에 필요한 이름/학번/학교/학과를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 완료 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = VerificationCompleteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 또는 유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 프로필 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/verification-complete")
    public VerificationCompleteResponse verificationComplete(@UserId Long userId) {
        return loginService.getVerificationCompleteInfo(userId);
    }

    @Operation(summary = "AccessToken 재발급", description = "유효한 refreshToken으로 새 accessToken을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 JSON",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "refreshToken 누락, 만료, 변조, 재사용 또는 허용되지 않는 토큰 타입",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "정지 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest req) {
        return ApiResponse.success(authTokenService.refreshAccessToken(req.refreshToken()));
    }

    @Operation(summary = "아이디 찾기", description = "이름과 이메일을 확인하여 가입된 아이디를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "아이디 찾기 성공",
                    content = @Content(schema = @Schema(implementation = FindUsernameResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이름 또는 이메일 불일치",
                    content = @Content(schema = @Schema(oneOf = {
                            InvalidPropertiesErrorResponse.class,
                            ErrorResponse.class
                    }))
            )
    })
    @PostMapping("/username/find")
    @ResponseStatus(HttpStatus.OK)
    public FindUsernameResponse findUsername(@RequestBody FindUsernameRequest req) {
        return accountRecoveryService.findUsername(req);
    }

    @Operation(summary = "비밀번호 재설정 이메일 코드 전송", description = "가입된 이메일로 비밀번호 재설정용 6자리 인증 코드를 전송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 인증번호 전송 성공",
                    content = @Content(schema = @Schema(implementation = SendPasswordResetEmailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "아이디 또는 이메일 불일치",
                    content = @Content(schema = @Schema(oneOf = {
                            InvalidPropertiesErrorResponse.class,
                            ErrorResponse.class
                    }))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "정지 사용자 또는 이메일 미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/password/reset/email/send")
    @ResponseStatus(HttpStatus.OK)
    public SendPasswordResetEmailResponse sendPasswordResetEmail(@RequestBody SendPasswordResetEmailRequest req) {
        long expiresMinutes = emailVerificationService.sendPasswordResetCode(req.username(), req.email());
        return new SendPasswordResetEmailResponse(req.email(), expiresMinutes);
    }

    @Operation(summary = "비밀번호 재설정 인증번호 확인", description = "인증 코드를 확인받고 resetToken을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증번호 검증 성공 및 resetToken 발급",
                    content = @Content(schema = @Schema(implementation = VerifyPasswordResetEmailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "활성 인증번호 없음, 만료/사용됨, 인증번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "정지 사용자 또는 이메일 미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "인증번호 시도 횟수 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/password/reset/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public VerifyPasswordResetEmailResponse verifyPasswordResetEmail(
            @RequestBody @Valid VerifyPasswordResetEmailRequest req
    ) {
        return emailVerificationService.verifyPasswordResetEmail(req);
    }

    @Operation(summary = "비밀번호 재설정", description = "검증된 resetToken으로 비밀번호를 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "비밀번호 재설정 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비밀번호 형식 오류 또는 기존 비밀번호와 동일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "resetToken 누락, 만료, 변조 또는 허용되지 않는 토큰 타입",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        emailVerificationService.resetPassword(req);
    }

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 탈퇴 처리(익명화)합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "회원 탈퇴 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 또는 유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @UserId Long userId,
            @RequestBody @Valid WithdrawRequest req
    ) {
        loginService.withdraw(userId, req);
    }
}
