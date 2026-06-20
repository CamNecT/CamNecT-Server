package CamNecT.server.domain.report.repository;

import CamNecT.server.domain.report.model.Report;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 관리자용: 타입과 상태에 따른 페이징 조회
    Page<Report> findAllByPostTypeAndStatus(TargetType postType, ReportStatus status, Pageable pageable);

    // 관리자용: 상태에 따른 페이징 조회 (타입 무관)
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);
}