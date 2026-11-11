package CamNecT.server.domain.activity.repository.external_activity;

import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalActivityRepository extends JpaRepository<ExternalActivity, Long>, ExternalActivityRepositoryCustom {

    interface GuestActivityRow {
        Long getActivityId();
        String getTitle();
        String getThumbnailKey();
    }

    @Query("""
            select a.activityId as activityId, a.title as title, a.thumbnailKey as thumbnailKey
            from ExternalActivity a
            where (:category is null or a.category = :category)
              and (:title is null or lower(a.title) like lower(concat('%', :title, '%')))
            order by a.createdAt desc, a.activityId desc
            """)
    Slice<GuestActivityRow> findGuestActivities(@Param("category") ActivityCategory category,
            @Param("title") String title, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE external_activities SET view_count = view_count + 1 WHERE activity_id = :id", nativeQuery = true)
    int incrementViews(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from ExternalActivity e where e.activityId = :id")
    Optional<ExternalActivity> findByIdForRead(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExternalActivity e where e.activityId = :id")
    Optional<ExternalActivity> findByIdForUpdate(@Param("id") Long id);

    @Query("select e.title from ExternalActivity e where e.activityId = :id")
    Optional<String> findTitleByActivityId(@Param("id") Long id);

}
