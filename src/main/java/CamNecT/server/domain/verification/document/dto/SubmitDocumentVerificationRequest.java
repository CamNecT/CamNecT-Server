package CamNecT.server.domain.verification.document.dto;

import CamNecT.server.domain.verification.document.model.DocumentType;
import jakarta.validation.constraints.*;

public record SubmitDocumentVerificationRequest(
        @NotNull DocumentType docType,
        @NotEmpty String documentKey
) {}
