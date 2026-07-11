package CamNecT.server.domain.verification.document.event;

import CamNecT.server.global.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DocumentVerificationMailListener {

    private final EmailSender emailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DocumentVerificationReviewedEvent e) {
        emailSender.sendDocumentVerificationResult(
                e.toEmail(),
                e.docType(),
                e.decision(),
                e.reason()
        );
    }
}