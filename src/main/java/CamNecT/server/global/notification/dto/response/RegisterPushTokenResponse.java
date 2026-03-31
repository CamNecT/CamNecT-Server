package CamNecT.server.global.notification.dto.response;

public record RegisterPushTokenResponse(
        Long pushDeviceId,
        boolean created
) {}