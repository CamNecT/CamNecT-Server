package CamNecT.server.domain.report.controller;

import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.dto.request.ReportProcessRequest;
import CamNecT.server.domain.report.dto.response.ReportResponse;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import CamNecT.server.domain.report.service.ReportService;
import CamNecT.server.global.common.auth.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 일반 유저가 신고하는 메서드
    @PostMapping
    public ResponseEntity<Long> createReport(@RequestBody ReportCreateRequest request) {
        Long reporterId = 1L;
        Long reportId = reportService.createReport(reporterId, request);
        return ResponseEntity.ok(reportId);
    }

    // 관리자가 신고 목록을 조회하는 메서드
    @GetMapping("/admin")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @UserId Long userId,
            @RequestParam(required = false) TargetType type,
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(reportService.findAllReports(userId, type, status, pageable));
    }

    // 관리자가 신고를 처리하는 메서드
    @PatchMapping("/admin/{reportId}/status")
    public ResponseEntity<Void> processReport(
            @UserId Long userId,
            @PathVariable Long reportId,
            @RequestBody ReportProcessRequest request) {

        reportService.processReport(userId, reportId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}