package CamNecT.server.domain.activity.repository.recruitment;

import CamNecT.server.domain.activity.model.recruitment.TeamApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {

    boolean existsByRecruitIdAndUserId(Long recruitId, Long userId);

    void deleteByRecruitId(Long recruitId);

    @Modifying
    @Query("""
            update TeamApplication a set a.status = REJECTED
            where a.status = REQUESTED and exists (
                select q.id from ChatRequest q where q.id in :waitingIds
                  and q.recruitmentId = a.recruitId and q.requester.userId = a.userId)
            """)
    int rejectApplicationsForRequests(@Param("waitingIds") List<Long> waitingIds);
}
