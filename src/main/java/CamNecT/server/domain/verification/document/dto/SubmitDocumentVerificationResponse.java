package CamNecT.server.domain.verification.document.dto;

import CamNecT.server.domain.verification.document.model.VerificationStatus;

import java.time.LocalDateTime;

public record SubmitDocumentVerificationResponse(
        Long submissionId,
        VerificationStatus status,
        LocalDateTime submittedAt
) {}
