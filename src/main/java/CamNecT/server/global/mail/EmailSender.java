package CamNecT.server.global.mail;

import CamNecT.server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.server.domain.verification.document.model.DocumentType;

public interface EmailSender {
    void sendEmailVerificationCode(String toEmail, String code, long expiresMinutes);

    void sendDocumentVerificationResult(String toEmail,
                                        DocumentType docType,
                                        AdminReviewDocumentVerificationRequest.Decision decision,
                                        String reason);
}
