package CamNecT.CamNecT_Server.domain.alumni.dto.response;

import CamNecT.CamNecT_Server.domain.alumni.dto.UserProfileDto;
import CamNecT.CamNecT_Server.global.tag.model.Tag;

import java.util.List;

public record AlumniPreviewResponse(
        Long userId,
        String userName,
        UserProfileDto userProfile,
        List<String> tagList
) {
}
