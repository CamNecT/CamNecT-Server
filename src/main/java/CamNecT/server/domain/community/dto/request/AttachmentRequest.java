package CamNecT.server.domain.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttachmentRequest(
        @NotBlank @Size(max = 500) String fileKey,
        Integer width,
        Integer height,
        Long fileSize
) {}