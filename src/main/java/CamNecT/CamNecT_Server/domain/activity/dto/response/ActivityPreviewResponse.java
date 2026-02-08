package CamNecT.CamNecT_Server.domain.activity.dto.response;

import CamNecT.CamNecT_Server.global.tag.model.Tag;

import java.util.List;

public record ActivityPreviewResponse(
        Long activityId,
        String title,
        String context,
        String thumbnailUrl,
        List<String> tags
) {
}
