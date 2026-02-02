package CamNecT.CamNecT_Server.domain.auth.controller;

import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.login.LoginResponse;
import CamNecT.CamNecT_Server.domain.auth.dto.signup.SignupRequest;
import CamNecT.CamNecT_Server.domain.auth.dto.signup.SignupResponse;
import CamNecT.CamNecT_Server.domain.auth.service.LoginService;
import CamNecT.CamNecT_Server.domain.auth.service.SignupService;
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

    private final SignupService signupService;
    private final LoginService loginService;

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

    @Operation(
            summary = "회원가입",
            description = "회원가입 정보를 저장합니다. (약관 동의/중복 체크/비밀번호 조건 등 포함)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User객체 생성 및 이메일 전송 성공",
                    content = @Content(schema = @Schema(implementation = SignupResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 또는 약관 미동의 등", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복(이메일/아이디 등)", content = @Content)
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody @Valid SignupRequest req) {
        return signupService.signup(req);
    }
}