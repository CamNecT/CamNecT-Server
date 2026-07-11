package CamNecT.server.global.notification.dto.request;

import CamNecT.server.global.notification.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPushTokenRequest(
        @NotBlank @Size(max = 128) String deviceId,
        @NotNull Platform platform,
        @NotBlank @Size(max = 512) String token
) {}
