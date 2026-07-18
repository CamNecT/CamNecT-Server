package CamNecT.server.domain.report.repository;

import CamNecT.server.domain.report.model.Report;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 관리자용: 타입과 상태에 따른 페이징 조회
    Page<Report> findAllByPostTypeAndStatus(TargetType postType, ReportStatus status, Pageable pageable);

    // 관리자용: 상태에 따른 페이징 조회 (타입 무관)
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    // 특정 유저에 대한 신고 누적 수 조회
    long countByReportedUserIdAndStatus(Long reportedUserId, ReportStatus status);

    // 특정 유저에 대한 모든 승인된 신고 조회
    @Query("SELECT r FROM Report r WHERE r.reportedUserId = :userId AND r.status = 'RESOLVED' ORDER BY r.createdAt DESC")
    List<Report> findResolvedReportsByUserId(@Param("userId") Long userId);
}