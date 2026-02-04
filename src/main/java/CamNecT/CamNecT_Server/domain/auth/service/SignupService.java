package CamNecT.CamNecT_Server.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupService {
    // 현재는 EmailVerificationService가 send/verify를 모두 담당합니다.
    // 추후 “유저 생성 책임”만 SignupService로 분리하고 싶어지면,
    // EmailVerificationService.verifySignupAndCreateUser() 내부의 user 생성/유니크검증 로직을 이쪽으로 옮기면 됩니다.
}
