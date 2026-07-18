package CamNecT.server.domain.report.dto.response;

import CamNecT.server.domain.report.model.*;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long reporterId,
        Long reportedUserId,
        Long reportedPostId,
        TargetType postType,
        ReportCategory reportCategory,
        String title,
        String context,
        String evidenceImageUrl,
        ReportStatus status,
        PenaltyType appliedPenalty,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 엔티티 -> Record 변환 메서드
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getReportId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReportedPostId(),
                report.getPostType(),
                report.getReportCategory(),
                report.getTitle(),
                report.getContext(),
                report.getEvidenceImageUrl(),
                report.getStatus(),
                report.getAppliedPenalty(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}