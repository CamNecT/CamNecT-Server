package CamNecT.CamNecT_Server.domain.activity.dto.response;

import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.profile.dto.ProfileGlobalDto;

public record RecruitmentDetailResponse(
        ProfileGlobalDto profilePreview,
        TeamRecruitment recruitment,
        boolean isMine,
        boolean isBookmarked
) {
}
