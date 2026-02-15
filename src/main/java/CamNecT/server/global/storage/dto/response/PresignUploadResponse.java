package CamNecT.server.global.storage.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record PresignUploadResponse(
        String fileKey,
        String uploadUrl,
        LocalDateTime expiresAt,
        Map<String, String> requiredHeaders
) {}