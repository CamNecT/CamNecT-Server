package CamNecT.server.domain.verification.document.event;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentVerificationMailListener {

    private final EmailSender emailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DocumentVerificationReviewedEvent e) {
        try {
            emailSender.sendDocumentVerificationResult(
                    e.toEmail(),
                    e.docType(),
                    e.decision(),
                    e.reason()
            );
        } catch (CustomException ex) {
            log.warn("[mail] document verification result delivery failed. decision={}, email={}",
                    e.decision(), e.toEmail(), ex);
        }
    }
}
