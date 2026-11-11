package CamNecT.server.domain.preview;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.enums.ActivityStatus;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.global.tag.dto.TagDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GuestActivityDetail(Long id, ActivityCategory category, String title, String contextTitle,
        String content, String organizer, String region, String targetDescription, String thumbnailUrl,
        String officialUrl, LocalDate applyStartDate, LocalDate applyEndDate, Instant applyEndAt,
        LocalDate resultAnnounceDate, ActivityStatus status, LocalDateTime createdAt, List<TagDto> tags) {
    public static GuestActivityDetail from(ExternalActivity a, String thumbnailUrl, List<TagDto> tags) {
        return new GuestActivityDetail(a.getActivityId(), a.getCategory(), a.getTitle(), a.getContextTitle(),
                a.getContext(), a.getOrganizer(), a.getRegion(), a.getTargetDescription(), thumbnailUrl,
                a.getOfficialUrl(), a.getApplyStartDate(), a.getApplyEndDate(), a.getApplyEndAt(),
                a.getResultAnnounceDate(), a.getStatus(), a.getCreatedAt(), tags);
    }
}
