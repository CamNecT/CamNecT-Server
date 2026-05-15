package CamNecT.server.domain.report.service;

import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.dto.response.ReportResponse;
import CamNecT.server.domain.report.model.Report;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import CamNecT.server.domain.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    // 필요에 따라 각 도메인의 Repository를 주입받습니다.
    // private final CommunityRepository communityRepository;
    // private final ActivityRepository activityRepository;

    /**
     * 1. 신고 접수
     */
    @Transactional
    public Long createReport(Long reporterId, ReportCreateRequest dto) {
        Report report = new Report(
                reporterId,
                dto.reportedUserId(),
                dto.reportedPostId(),
                dto.postType(),
                dto.reportCategory(),
                dto.title(),
                dto.context()
        );
        return reportRepository.save(report).getReportId();
    }

    /**
     * 2. 관리자용 목록 조회
     */
    public Page<ReportResponse> findAllReports(TargetType type, ReportStatus status, Pageable pageable) {
        Page<Report> reports;

        if (type != null && status != null) {
            reports = reportRepository.findAllByPostTypeAndStatus(type, status, pageable);
        } else if (status != null) {
            reports = reportRepository.findAllByStatus(status, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }

        return reports.map(ReportResponse::from);
    }

    /**
     * 3. 관리자 신고 처리 (승인/반려)
     */
    @Transactional
    public void processReport(Long reportId, ReportStatus newStatus) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고가 존재하지 않습니다."));

        // 상태 업데이트
        report.updateStatus(newStatus);

        // 만약 '처리 완료(RESOLVED)' 상태로 변경 시 추가 로직 실행
        if (newStatus == ReportStatus.RESOLVED) {
            handleViolation(report);
        }
    }

    private void handleViolation(Report report) {
        // 다형성 구조에 따른 분기 처리
        switch (report.getPostType()) {
            case COMMUNITY -> {
                // communityRepository.findById(report.getReportedPostId())를 통한 블라인드 처리 로직
            }
            case ACTIVITY -> {
                // activityRepository.findById(report.getReportedPostId())를 통한 블라인드 처리 로직
            }
            case USER -> {
                // 유저 경고 횟수 누적 또는 정지 로직
            }
        }
    }
}