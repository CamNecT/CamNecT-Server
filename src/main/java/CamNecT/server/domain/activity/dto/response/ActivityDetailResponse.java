package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.server.domain.activity.dto.ExternalActivityDto;
import CamNecT.server.domain.activity.dto.TeamRecruitmentDto;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;

import java.util.List;

public record ActivityDetailResponse(
        boolean isMine,
        ProfileGlobalDto profilePreview,
        ExternalActivityDto activity,
        List<ExternalActivityAttachmentDto> attachment,
        List<String> tagList,
        List<TeamRecruitmentDto> recruitmentList,
        Long bookmarkCount,
        boolean isBookmarked
        ) {
}
