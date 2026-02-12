package CamNecT.CamNecT_Server.domain.activity.dto;

import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityStatus;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExternalActivityDto {

    private final Long activityId;
    private final String title;
    private final ActivityCategory category;
    private final String organizer;
    private final Long userId; // DTO는 그대로 Long 유지 가능
    private final String region;
    private final String targetDescription;
    private final String thumbnailUrl;
    private final LocalDate applyStartDate;
    private final LocalDate applyEndDate;
    private final LocalDate resultAnnounceDate;
    private final String officialUrl;
    private final ActivityStatus status;
    private final LocalDateTime createdAt;
    private final String contextTitle;
    private final String context;

    public static ExternalActivityDto from(ExternalActivity entity) {
        Long userId = (entity.getUser() == null) ? null : entity.getUser().getUserId();

        return ExternalActivityDto.builder()
                .activityId(entity.getActivityId())
                .title(entity.getTitle())
                .category(entity.getCategory())
                .organizer(entity.getOrganizer())
                .userId(userId)
                .region(entity.getRegion())
                .targetDescription(entity.getTargetDescription())
                .thumbnailUrl(entity.getThumbnailUrl())
                .applyStartDate(entity.getApplyStartDate())
                .applyEndDate(entity.getApplyEndDate())
                .resultAnnounceDate(entity.getResultAnnounceDate())
                .officialUrl(entity.getOfficialUrl())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .contextTitle(entity.getContextTitle())
                .context(entity.getContext())
                .build();
    }

    public ExternalActivityDto withThumbnailUrl(String thumbnailUrl) {
        return ExternalActivityDto.builder()
                .activityId(activityId)
                .title(title)
                .category(category)
                .organizer(organizer)
                .userId(userId)
                .region(region)
                .targetDescription(targetDescription)
                .thumbnailUrl(thumbnailUrl)
                .applyStartDate(applyStartDate)
                .applyEndDate(applyEndDate)
                .resultAnnounceDate(resultAnnounceDate)
                .officialUrl(officialUrl)
                .status(status)
                .createdAt(createdAt)
                .contextTitle(contextTitle)
                .context(context)
                .build();
    }
}