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

    /**
     * 1. 일반 유저가 신고를 접수하는 메서드
     */
    @PostMapping
    public ResponseEntity<Long> createReport(@RequestBody ReportCreateRequest request) {
        // 실제 구현 시에는 SecurityContext에서 현재 로그인한 유저 ID를 가져와 사용합니다.
        Long reporterId = 1L;
        Long reportId = reportService.createReport(reporterId, request);
        return ResponseEntity.ok(reportId);
    }

    /**
     * 2. 관리자가 신고 목록을 조회하는 메서드
     * (타입별 필터링과 페이징 처리를 포함하는 것이 좋습니다.)
     */
    @GetMapping("/admin")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @UserId Long userId,
            @RequestParam(required = false) TargetType type,
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(reportService.findAllReports(userId, type, status, pageable));
    }

    /**
     * 3. 관리자가 들어온 신고를 처리(통과/반려)하는 메서드
     */
    @PatchMapping("/admin/{reportId}/status")
    public ResponseEntity<Void> processReport(
            @UserId Long userId,
            @PathVariable Long reportId,
            @RequestBody ReportProcessRequest request) {

        reportService.processReport(userId, reportId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}