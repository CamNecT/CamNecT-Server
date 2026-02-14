package CamNecT.server.global.notification.dto.response;

import CamNecT.server.global.notification.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationItemResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,

        Long actorUserId,
        String actorName,
        String actorProfileImageUrl,

        Long postId,
        Long commentId,
        Long requestId,
        String link,

        LocalDateTime createdAt
) {}