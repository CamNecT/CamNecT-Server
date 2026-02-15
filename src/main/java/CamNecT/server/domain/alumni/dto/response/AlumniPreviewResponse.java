package CamNecT.server.domain.alumni.dto.response;

import CamNecT.server.domain.alumni.dto.UserProfileDto;
import java.util.List;

public record AlumniPreviewResponse(
        Long userId,
        String userName,
        UserProfileDto userProfile,
        List<String> tagList
) {
}
