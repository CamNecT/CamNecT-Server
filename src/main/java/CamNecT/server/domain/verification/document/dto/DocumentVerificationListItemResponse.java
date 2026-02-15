package CamNecT.server.domain.verification.document.dto;

import CamNecT.server.domain.verification.document.model.DocumentType;
import CamNecT.server.domain.verification.document.model.VerificationStatus;

import java.time.LocalDateTime;

public record DocumentVerificationListItemResponse(
        Long submissionId,
        DocumentType docType,
        VerificationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {}
