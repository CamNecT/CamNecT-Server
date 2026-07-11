package CamNecT.server.domain.auth.controller;

import CamNecT.server.domain.auth.dto.login.LoginRequest;
import CamNecT.server.domain.auth.dto.login.LoginResponse;
import CamNecT.server.domain.auth.dto.login.VerificationCompleteResponse;
import CamNecT.server.domain.auth.dto.others.TokenRefreshResponse;
import CamNecT.server.domain.auth.dto.others.WithdrawRequest;
import CamNecT.server.domain.auth.dto.signup.*;
import CamNecT.server.domain.auth.service.LoginService;
import CamNecT.server.domain.profile.dto.request.UpdateOnboardingRequest;
import CamNecT.server.domain.profile.dto.response.ProfileStatusResponse;
import CamNecT.server.domain.profile.service.ProfileService;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.verification.email.service.EmailVerificationService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(
            summary = "로그인",
            description = "아이디/비밀번호로 로그인하고 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 아이디 또는 비밀번호 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "41101 아이디 또는 비밀번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "41302 정지된 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 토큰 발급·저장 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest req) {
        return loginService.login(req);
    }



    @Operation(summary = "아이디 중복확인", description = "사용 가능하면 true, 이미 사용 중이면 false")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 이메일 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "41901 이미 가입된 이메일", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 인증코드 저장 또는 내부 오류 (메일 발송 실패는 AFTER_COMMIT 처리로 이 응답에 포함되지 않음)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 41010 비밀번호 정책 위반 / 41020 필수 약관 미동의 / 42030 활성 코드 없음 / 42031 만료·사용된 코드 / 42032 코드 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "41901 이메일 / 41902 아이디 / 41903 전화번호 중복 / 41904 DB 유니크 충돌", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "42920 인증코드 시도 횟수 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 사용자·프로필 저장, 임시 토큰 발급 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패 / 44030 유효하지 않은 태그 / 49010 만료·사용된 업로드 티켓 / 49011 업로드 파일 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "41102 Bearer 형식 오류 / 41103 유효하지 않은 토큰 / 41104 토큰 누락 / 41106 허용되지 않은 토큰 타입", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "41301 이메일 미인증 / 41302 정지된 사용자 / 49310 업로드 티켓 사용 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "44402 사용자 프로필 / 49410 업로드 티켓 / 49401 업로드 파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "49902 업로드 파일 확인 실패 / 49904 파일 이동 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "41102 Bearer 형식 오류 / 41103 유효하지 않은 토큰 / 41104 토큰 누락 / 41106 Access Token이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 로그아웃 처리 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public void logout(@UserId Long loginUserId) {
        loginService.logout(loginUserId);
    }

    @Operation(summary = "인증 완료 화면 정보 조회", description = "인증 완료 화면에 필요한 이름/학번/학교/학과를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "41102 Bearer 형식 오류 / 41103 유효하지 않은 토큰 / 41104 토큰 누락 / 41106 Access Token이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "44402 사용자 프로필을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/verification-complete")
    public VerificationCompleteResponse verificationComplete(@UserId Long userId) {
        return loginService.getVerificationCompleteInfo(userId);
    }

    @Operation(summary = "Refresh token 재발급 비활성화", description = "현재 refresh token 재발급 기능은 비활성화되어 있으며, 호출하면 410 Gone을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "41000 Refresh token 재발급 API 비활성화", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh() {
        throw new CustomException(ErrorCode.GONE);
    }

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 탈퇴 처리(익명화)합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "41101 현재 비밀번호 불일치 / 41102 Bearer 형식 오류 / 41103 유효하지 않은 토큰 또는 토큰 사용자 없음 / 41104 토큰 누락 / 41106 Access Token이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "50000 탈퇴 데이터 정리 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
