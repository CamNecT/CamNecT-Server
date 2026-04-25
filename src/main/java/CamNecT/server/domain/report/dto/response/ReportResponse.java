package CamNecT.server.domain.report.dto.response;

import CamNecT.server.domain.report.model.Report;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long reporterId,
        Long reportedUserId,
        Long reportedPostId,
        TargetType postType,
        String reportCategory,
        String title,
        String context,
        ReportStatus status,
        LocalDateTime createdAt
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
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}