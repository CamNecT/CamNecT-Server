package CamNecT.server.domain.report.service;

import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.dto.response.ReportResponse;
import CamNecT.server.domain.report.model.Report;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import CamNecT.server.domain.report.repository.ReportRepository;

import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ReportErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
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
    private final UserRepository userRepository;

    // 관리자 검증 공통 메서드
    private void validateAdmin(Long userId) {
        Users adminUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (adminUser.getRole() != UserRole.ADMIN) throw new CustomException(UserErrorCode.USER_NOT_ADMIN);
    }

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
    public Page<ReportResponse> findAllReports(Long userId, TargetType type, ReportStatus status, Pageable pageable) {
        validateAdmin(userId);

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
    public void processReport(Long userId, Long reportId, ReportStatus newStatus) {
        validateAdmin(userId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

        report.updateStatus(newStatus);

        if (newStatus == ReportStatus.RESOLVED) {
            handleViolation(report);
        }
    }

    private void handleViolation(Report report) {
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