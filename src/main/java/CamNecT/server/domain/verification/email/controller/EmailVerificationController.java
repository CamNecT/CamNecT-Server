package CamNecT.server.domain.verification.email.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verification")
public class EmailVerificationController {

// 현재 이메일 인증관련 service 호출관련, 구현은 EmailVerificationService에 되어있으나 호출은 AuthController 담당입니다.
// 추후 인증 이메일 발송 확장이 생기면(비밀번호 재발급, 민감정보 수정 등) 그때를 위해서 형식만 남겨둡니다.
}
