package CamNecT.server.domain.community.dto.response;

public record PostAttachmentItemResponse(
        Long attachmentId,
        int sortOrder,
        String fileKey,
        String downloadUrl,
        Integer width,
        Integer height,
        Long fileSize
) {}