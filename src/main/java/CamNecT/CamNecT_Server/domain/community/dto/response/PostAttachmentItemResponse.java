package CamNecT.CamNecT_Server.domain.community.dto.response;

public record PostAttachmentItemResponse(
        Long attachmentId,
        int sortOrder,
        String fileKey,
        Integer width,
        Integer height,
        Long fileSize
) {}