package CamNecT.server.domain.activity.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ActivityImportRepository extends JpaRepository<ActivityImport, String> {
    // A locking read is also a current read under MySQL REPEATABLE READ.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ActivityImport i where i.sourceId = :id")
    Optional<ActivityImport> findByIdForUpdate(@Param("id") String id);

    @Query("""
        select i.sourceId from ActivityImport i, ExternalActivity a
        where i.activityId = a.activityId and i.sourceId > :after
          and a.status = CamNecT.server.domain.activity.model.enums.ActivityStatus.OPEN
          and ((a.applyEndAt is not null and a.applyEndAt <= :now)
            or (a.applyEndAt is null and a.applyEndDate < :today))
        order by i.sourceId
        """)
    List<String> findExpiredCandidates(@Param("after") String after, @Param("now") Instant now,
                                       @Param("today") LocalDate today, Pageable pageable);
}
