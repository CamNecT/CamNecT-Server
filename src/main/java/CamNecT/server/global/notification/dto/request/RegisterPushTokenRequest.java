package CamNecT.server.global.notification.dto.request;

import CamNecT.server.global.notification.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPushTokenRequest(
        @NotBlank String deviceId,
        @NotNull Platform platform,
        @NotBlank String token
) {}
