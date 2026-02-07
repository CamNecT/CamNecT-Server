package CamNecT.CamNecT_Server.domain.verification.document.dto;

import jakarta.validation.constraints.NotNull;

public record AdminReviewDocumentVerificationRequest(
        @NotNull Decision decision,
        String reason,

        // 승인 시 관리자 입력값(승인 요청일 때만 필수 처리)
        String studentName,
        String studentNo,
        Long institutionId,
        Long majorId
) {
    public enum Decision { APPROVE, REJECT }
}
