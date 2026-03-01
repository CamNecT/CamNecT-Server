package CamNecT.server.domain.activity.repository.external_activity;

import CamNecT.server.domain.activity.model.external_activity.ExternalActivityAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalActivityAttachmentRepository extends JpaRepository<ExternalActivityAttachment, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExternalActivityAttachment a where a.activity.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);

    List<ExternalActivityAttachment> findAllByActivity_ActivityId(Long activityId);
}
