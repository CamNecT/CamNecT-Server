package CamNecT.server.domain.report.service;

import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.dto.response.ReportResponse;
import CamNecT.server.domain.report.model.*;
import CamNecT.server.domain.report.repository.ReportRepository;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ReportErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PublicUrlIssuer publicUrlIssuer;
    private final ReportAttachmentService reportAttachmentService;

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
        // 신고 대상 사용자 존재 확인
        Users reportedUser = userRepository.findById(dto.reportedUserId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 증거 이미지는 일단 presign된 키로 저장, 나중에 consume 처리
        Report report = new Report(
                reporterId,
                dto.reportedUserId(),
                dto.reportedPostId(),
                dto.postType(),
                dto.reportCategory(),
                dto.title(),
                dto.context(),
                dto.evidenceImageUrl()
        );
        Report savedReport = reportRepository.save(report);
        
        // 증거 이미지가 있으면 최종 경로로 이동 (consume)
        if (StringUtils.hasText(dto.evidenceImageUrl())) {
            String finalEvidenceUrl = reportAttachmentService.applyOnReportCreate(
                    reporterId,
                    savedReport.getReportId(),
                    dto.evidenceImageUrl()
            );
            savedReport.updateEvidenceImageUrl(finalEvidenceUrl);
        }
        
        return savedReport.getReportId();
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
            applyPenalty(report);
        }
    }

    /**
     * 신고 기반 패널티 적용 로직
     * - 1회 접수: 경고 알림
     * - 2회 접수: 7일 정지
     * - 3회 접수: 영구 차단
     * - 즉시 영구 제재: 성희롱, 포교, 명백한 문서 위조 (1회 적발)
     */
    @Transactional
    protected void applyPenalty(Report report) {
        Users reportedUser = userRepository.findById(report.getReportedUserId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 영구 차단 대상 확인 (즉시 제재)
        if (report.getReportCategory().isImmediateBan()) {
            reportedUser.applyPermanentBan("즉시 제재 대상: " + report.getReportCategory().getDisplayName());
            report.applyPenalty(PenaltyType.PERMANENT_BAN);
            userRepository.save(reportedUser);
            return;
        }

        // 신고 누적 횟수 증가
        reportedUser.incrementReportCount();
        int reportCount = reportedUser.getReportCount();

        PenaltyType penaltyType;

        if (reportCount == 1) {
            // 1회: 경고 알림만 발송
            penaltyType = PenaltyType.WARNING;
            // TODO: 경고 알림 발송 로직
        } else if (reportCount == 2) {
            // 2회: 7일 정지
            penaltyType = PenaltyType.SUSPENDED_7_DAYS;
            LocalDateTime suspensionEndDate = LocalDateTime.now().plusDays(7);
            reportedUser.applySuspension(suspensionEndDate);
            // TODO: 7일 정지 알림 발송 로직
        } else if (reportCount >= 3) {
            // 3회 이상: 영구 차단
            penaltyType = PenaltyType.PERMANENT_BAN;
            reportedUser.applyPermanentBan("신고 누적 3회로 인한 영구 차단");
            // TODO: 영구 차단 알림 발송 로직
        } else {
            penaltyType = PenaltyType.WARNING;
        }

        report.applyPenalty(penaltyType);
        userRepository.save(reportedUser);
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    public ReportResponse getReportDetail(Long userId, Long reportId) {
        validateAdmin(userId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }

    /**
     * 특정 유저의 신고 누적 수 조회
     */
    public long getResolvedReportCount(Long userId) {
        return reportRepository.countByReportedUserIdAndStatus(userId, ReportStatus.RESOLVED);
    }

    /**
     * 증거 이미지 저장 키로부터 유효한 공개 URL 발급
     * 이미지 확장자 유효성 확인 후 공개 URL 반환
     */
    public String issueEvidenceImageUrl(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return null;
        }
        return publicUrlIssuer.issueImagePublicUrl(storageKey);
    }
}