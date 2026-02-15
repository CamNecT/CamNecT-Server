package CamNecT.server.domain.activity.repository.external_activity;

import CamNecT.server.domain.activity.model.external_activity.ExternalActivityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalActivityTagRepository extends JpaRepository<ExternalActivityTag, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExternalActivityTag e where e.activity.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);

    List<ExternalActivityTag> findAllByActivity_ActivityId(Long activityId);
    List<ExternalActivityTag> findAllByActivity_ActivityIdIn(List<Long> activityIds);
}