package CamNecT.server.domain.activity.repository.external_activity;

import CamNecT.server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;

public interface ExternalActivityRepositoryCustom {
    Optional<ExternalActivity> findVisibleOnApp(Long id);
    Slice<ActivityPreviewResponse> findActivitiesByCondition(
            Long userId,
            ActivityCategory category,
            List<Long> tagIds,
            String title,
            String sortType,
            Pageable pageable
    );

    // 홈: 북마크 많은 EXTERNAL activityId 상위 N(+1)개
    List<Long> findTopIdsByBookmark(ActivityCategory category, int limitPlusOne);
}