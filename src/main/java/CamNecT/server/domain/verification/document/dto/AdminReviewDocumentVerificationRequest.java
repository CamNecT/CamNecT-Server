package CamNecT.server.domain.verification.document.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReviewDocumentVerificationRequest(
        @NotNull Decision decision,
        @Size(max = 500) String reason,

        // 승인 시 관리자 입력값(승인 요청일 때만 필수 처리)
        @Size(max = 100) String studentName,
        @Size(max = 20) String studentNo,
        Long institutionId,
        Long majorId
) {
    public enum Decision { APPROVE, REJECT }
}
