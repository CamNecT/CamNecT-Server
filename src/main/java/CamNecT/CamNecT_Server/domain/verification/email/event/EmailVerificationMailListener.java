package CamNecT.CamNecT_Server.domain.verification.email.event;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationMailListener {

    private final EmailSender emailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EmailVerificationCodeIssuedEvent e) {
        try {
            emailSender.sendEmailVerificationCode(e.email(), e.code(), e.expiresMinutes());
        } catch (CustomException ex) {
            // 회원가입은 이미 커밋된 상태일 수 있으니, 여기서 예외를 다시 던지면 응답만 실패로 나갈 수 있습니다.
            log.warn("[mail] verification mail failed. email={}", e.email(), ex);
        }
    }
}
