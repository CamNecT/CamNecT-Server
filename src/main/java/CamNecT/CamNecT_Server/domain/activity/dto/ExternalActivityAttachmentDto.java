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
    private final String fileUrl;
    private final LocalDateTime createdAt;

    /**
     * 엔티티 → DTO 1:1 매핑
     */
    public static ExternalActivityAttachmentDto from(ExternalActivityAttachment entity) {
        return ExternalActivityAttachmentDto.builder()
                .id(entity.getId())
                .activityId(entity.getExternalActivity())
                .fileUrl(entity.getFileUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * presigned URL 적용용
     */
    public ExternalActivityAttachmentDto withFileUrl(String fileUrl) {
        return ExternalActivityAttachmentDto.builder()
                .id(id)
                .activityId(activityId)
                .fileUrl(fileUrl)
                .createdAt(createdAt)
                .build();
    }
}