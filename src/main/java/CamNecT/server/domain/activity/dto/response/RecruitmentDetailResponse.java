package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;

public record RecruitmentDetailResponse(
        AuthorDto author,
        TeamRecruitment recruitment,
        String activityTitle,
        boolean isMine,
        boolean isBookmarked
) {
}
