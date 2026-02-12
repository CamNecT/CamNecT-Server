package CamNecT.CamNecT_Server.domain.activity.dto;

import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivityAttachment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExternalActivityAttachmentDto {

    private final Long id;
    private final Long activityId;
    private final String fileKey;
    private final String fileUrl;
    private final LocalDateTime createdAt;

    public static ExternalActivityAttachmentDto from(ExternalActivityAttachment entity) {
        Long activityId = (entity.getActivity() == null) ? null : entity.getActivity().getActivityId();

        return ExternalActivityAttachmentDto.builder()
                .id(entity.getId())
                .activityId(activityId)
                .fileKey(entity.getFileKey())
                .fileUrl(null) // presign으로 채울 예정
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ExternalActivityAttachmentDto withFileUrl(String fileUrl) {
        return ExternalActivityAttachmentDto.builder()
                .id(id)
                .activityId(activityId)
                .fileKey(fileKey)
                .fileUrl(fileUrl)
                .createdAt(createdAt)
                .build();
    }
}