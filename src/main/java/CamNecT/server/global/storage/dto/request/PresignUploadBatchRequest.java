package CamNecT.server.global.storage.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignUploadBatchRequest(List<@Valid Item> items) {
    public record Item(
            String contentType,
            long size,
            @NotBlank @Size(max = 255) String originalFilename
    ) {}
}
