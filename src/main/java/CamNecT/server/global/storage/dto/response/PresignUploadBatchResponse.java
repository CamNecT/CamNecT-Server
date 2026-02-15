package CamNecT.server.global.storage.dto.response;

import java.util.List;

public record PresignUploadBatchResponse(List<PresignUploadResponse> items) {}
