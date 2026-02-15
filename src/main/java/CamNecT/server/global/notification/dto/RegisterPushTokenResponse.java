package CamNecT.server.global.notification.dto;

public record RegisterPushTokenResponse(
        Long pushDeviceId,
        boolean created
) {}