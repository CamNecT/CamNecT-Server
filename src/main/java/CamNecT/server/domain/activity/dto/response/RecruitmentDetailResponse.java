package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.community.dto.AuthorDto;

public record RecruitmentDetailResponse(
        AuthorDto author,
        TeamRecruitment recruitment,
        String activityTitle,
        boolean isMine,
        boolean isBookmarked
) {
}
