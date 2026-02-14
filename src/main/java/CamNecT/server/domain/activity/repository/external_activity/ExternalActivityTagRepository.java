package CamNecT.server.domain.activity.repository.external_activity;

import CamNecT.server.domain.activity.model.external_activity.ExternalActivityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalActivityTagRepository extends JpaRepository<ExternalActivityTag, Long> {

    void deleteByActivity_ActivityId(Long activityId);

    List<ExternalActivityTag> findAllByActivity_ActivityId(Long activityId);
    List<ExternalActivityTag> findAllByActivity_ActivityIdIn(List<Long> activityIds);
}