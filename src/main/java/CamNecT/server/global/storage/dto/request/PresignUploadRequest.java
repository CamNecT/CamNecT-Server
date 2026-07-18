package CamNecT.server.global.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresignUploadRequest(
        @NotBlank String contentType,
        @NotNull Long size,
        @NotBlank @Size(max = 255) String originalFilename
) {}
