package CamNecT.CamNecT_Server.global.storage.dto.request;

import java.util.List;

public record PresignUploadBatchRequest(List<Item> items) {
    public record Item(
            String contentType,
            long size,
            String originalFilename
    ) {}
}

