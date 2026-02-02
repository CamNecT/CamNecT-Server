package CamNecT.CamNecT_Server.domain.activity.dto.request;

import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;

import java.util.List;

public record ActivityRequest(
        ActivityCategory category,
        String title,
        List<Long> tagIds,
        String content,
        String thumbnailKey,
        List<String> attachmentKey
) {
}
