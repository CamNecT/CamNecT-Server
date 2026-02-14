package CamNecT.server.domain.verification.document.event;

import CamNecT.server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.server.domain.verification.document.model.DocumentType;

public record DocumentVerificationReviewedEvent(
        String toEmail,
        DocumentType docType,
        AdminReviewDocumentVerificationRequest.Decision decision,
        String reason
) {}