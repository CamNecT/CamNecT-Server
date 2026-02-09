package CamNecT.CamNecT_Server.domain.activity.repository.external_activity;

import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalActivityBookmarkRepository extends JpaRepository<ExternalActivityBookmark, Long> {

    Optional<ExternalActivityBookmark> findByUser_UserIdAndActivity_ActivityId(Long userId, Long activityId);

    // =========================
    // 북마크한 대외활동 목록 - 최신순
    // return: [0]=ExternalActivity, [1]=bookmarkCnt
    // =========================
    @Query("""
      select a,
             (select count(b2) from ExternalActivityBookmark b2 where b2.activity = a) as bookmarkCnt
      from ExternalActivityBookmark ub
      join ub.activity a
      where ub.user.userId = :userId
        and (:cursorId is null or a.activityId < :cursorId)
      order by a.activityId desc
    """)
    Slice<Object[]> findBookmarkedActivitiesLatestWithCounts(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // =========================
    // 북마크한 대외활동 목록 - 추천순(북마크 수 desc)
    // cursorValue = bookmarkCnt 커서
    // return: [0]=ExternalActivity, [1]=bookmarkCnt
    // =========================
    @Query("""
      select a,
             count(b2) as bookmarkCnt
      from ExternalActivityBookmark ub
      join ub.activity a
      join ExternalActivityBookmark b2 on b2.activity = a
      where ub.user.userId = :userId
      group by a
      having (
          :cursorValue is null
          or count(b2) < :cursorValue
          or (count(b2) = :cursorValue and a.activityId < :cursorId)
      )
      order by count(b2) desc, a.activityId desc
    """)
    Slice<Object[]> findBookmarkedActivitiesRecommendedWithCounts(
            @Param("userId") Long userId,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // =========================
    // 내가 작성한 대외활동 목록 - 최신순 (+북마크수)
    // return: [0]=ExternalActivity, [1]=bookmarkCnt
    // =========================
    @Query("""
      select a,
             (select count(b2) from ExternalActivityBookmark b2 where b2.activity = a) as bookmarkCnt
      from ExternalActivity a
      where a.user.userId = :userId
        and (:cursorId is null or a.activityId < :cursorId)
      order by a.activityId desc
    """)
    Slice<Object[]> findMyActivitiesLatestWithCounts(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // =========================
    // 내가 작성한 대외활동 목록 - 추천순(북마크 수 desc)
    // cursorValue = bookmarkCnt 커서
    // return: [0]=ExternalActivity, [1]=bookmarkCnt
    // =========================
    @Query("""
    select a,
         count(b2) as bookmarkCnt
    from ExternalActivity a
    left join ExternalActivityBookmark b2 on b2.activity = a
    where a.user.userId = :userId
    group by a
    having (
         :cursorValue is null
         or count(b2) < :cursorValue
         or (count(b2) = :cursorValue and a.activityId < :cursorId)
    )
    order by count(b2) desc, a.activityId desc
    """)
    Slice<Object[]> findMyActivitiesRecommendedWithCounts(
            @Param("userId") Long userId,
            @Param("cursorValue") Long cursorValue,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

}
