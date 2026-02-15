package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.server.domain.activity.dto.ExternalActivityDto;
import CamNecT.server.domain.activity.dto.TeamRecruitmentDto;
import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;

import java.util.List;

public record ActivityDetailResponse(
        boolean isMine,
        AuthorDto author,
        ExternalActivityDto activity,
        List<ExternalActivityAttachmentDto> attachment,
        List<Long> tagIds,
        List<TeamRecruitmentDto> recruitmentList,
        Long bookmarkCount,
        boolean isBookmarked
        ) {
}
