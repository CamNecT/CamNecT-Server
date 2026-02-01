package CamNecT.CamNecT_Server.domain.activity.dto.response;

import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityAttachmentDto;
import CamNecT.CamNecT_Server.domain.activity.dto.ExternalActivityDto;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.global.tag.model.Tag;

import java.util.List;

public record ActivityDetailResponse(
        boolean isMine,
        ExternalActivityDto activity,
        List<ExternalActivityAttachmentDto> attachment,
        List<Tag> tagList,
        List<TeamRecruitment> recruitmentList
        ) {
}
