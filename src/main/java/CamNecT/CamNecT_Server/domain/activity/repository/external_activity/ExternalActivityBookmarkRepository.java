package CamNecT.CamNecT_Server.domain.activity.repository.external_activity;

import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalActivityBookmarkRepository extends JpaRepository<ExternalActivityBookmark, Long> {

    Optional<ExternalActivityBookmark> findByUser_UserIdAndActivity_ActivityId(Long userId, Long activityId);
    Long countByActivity_ActivityId(Long activityId);
    boolean existsByUser_UserIdAndActivity_ActivityId(Long userId, Long activityId);

}
