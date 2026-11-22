package CamNecT.server.domain.report.repository;

import CamNecT.server.domain.report.model.PenaltyType;
import CamNecT.server.domain.report.model.UserReportPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReportPenaltyRepository extends JpaRepository<UserReportPenalty, Long> {

    boolean existsByReportCase_CaseId(Long caseId);

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM user_report_penalty WHERE user_id = :userId)
                 + COALESCE((SELECT penalty_count FROM expired_penalty_summary WHERE user_id = :userId), 0)
            """, nativeQuery = true)
    long countByUser_UserId(@Param("userId") Long userId);

    @Query("""
            select case when count(p) > 0 or exists (select s.userId from ExpiredPenaltySummary s
                where s.userId = :userId and s.permanentlyBanned = true) then true else false end
            from UserReportPenalty p
            where p.user.userId = :userId
              and (
                    p.penaltyType = :permanentBan
                    or (p.penaltyType = :temporarySuspension and p.suspensionEndDate > :now)
              )
            """)
    boolean existsActiveRestriction(
            @Param("userId") Long userId,
            @Param("permanentBan") PenaltyType permanentBan,
            @Param("temporarySuspension") PenaltyType temporarySuspension,
            @Param("now") LocalDateTime now
    );

    List<UserReportPenalty> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
