package CamNecT.CamNecT_Server.domain.auth.controller;

import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginResponse;
import CamNecT.CamNecT_Server.domain.auth.dto.signup.*;
import CamNecT.CamNecT_Server.domain.auth.service.LoginService;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.verification.email.dto.VerifyEmailCodeResponse;
import CamNecT.CamNecT_Server.domain.verification.email.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(
            summary = "로그인",
            description = "아이디/비밀번호로 로그인하고 토큰을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패(아이디/비밀번호 불일치 등)", content = @Content)
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
            @ApiResponse(
                    responseCode = "200",
                    description = "인증코드 발송 성공",
                    content = @Content(schema = @Schema(implementation = SendSignupEmailResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = @Content)
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
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공 및 유저 생성 완료",
                    content = @Content(schema = @Schema(implementation = VerifyEmailCodeResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패/인증코드 불일치/약관 미동의/비밀번호 정책 위반 등", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복(이메일/아이디/전화번호)으로 유저 생성 불가", content = @Content)
    })
    @PostMapping("/signup/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public VerifySignupEmailResponse verifySignupEmail(@RequestBody @Valid VerifySignupEmailRequest req) {
        return emailVerificationService.verifySignupAndCreateUser(req);
    }
}