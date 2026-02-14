package CamNecT.CamNecT_Server.global.notification.dto;

public record NotificationPushPayload(
        String type,
        String title,
        String body,
        Long postId,
        Long commentId,
        Long requestId,
        String link
) {}