package CamNecT.CamNecT_Server.domain.activity.dto.response;

import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityDto;
import CamNecT.CamNecT_Server.domain.activity.dto.TeamRecruitmentDto;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.profile.dto.ProfileGlobalDto;

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
