package CamNecT.server.global.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignUploadRequest(
        @NotBlank String contentType,
        @NotNull Long size,
        @NotBlank String originalFilename
) {}