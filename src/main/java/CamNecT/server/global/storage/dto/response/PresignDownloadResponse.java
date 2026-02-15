package CamNecT.server.global.storage.dto.response;

import java.time.LocalDateTime;

public record PresignDownloadResponse(
        String downloadUrl,
        LocalDateTime expiresAt,
        String fileKey
) {}