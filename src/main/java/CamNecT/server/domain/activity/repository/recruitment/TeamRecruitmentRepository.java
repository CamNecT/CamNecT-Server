package CamNecT.server.domain.activity.repository.recruitment;

import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRecruitmentRepository extends JpaRepository<TeamRecruitment, Long> {

    @Modifying
    @Query(value = "UPDATE team_recruitments SET view_count = view_count + 1 WHERE recruit_id = :id", nativeQuery = true)
    int incrementViews(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select r from TeamRecruitment r where r.recruitId = :recruitId")
    Optional<TeamRecruitment> findByIdForRead(@Param("recruitId") Long recruitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TeamRecruitment r where r.recruitId = :recruitId")
    Optional<TeamRecruitment> findByIdForUpdate(@Param("recruitId") Long recruitId);

    @Query("select r.userId from TeamRecruitment r where r.recruitId = :recruitId")
    Optional<Long> findUserIdByRecruitId(@Param("recruitId") Long recruitId);

    boolean existsByActivityId(Long activityId);

    List<TeamRecruitment> findAllByActivityId(Long activityId);
}
