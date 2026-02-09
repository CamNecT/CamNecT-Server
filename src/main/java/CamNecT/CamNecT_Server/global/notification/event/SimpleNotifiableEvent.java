package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.model.NotificationType;

public record SimpleNotifiableEvent(
        Long receiverUserId,
        Long actorUserId,
        NotificationType type,
        String message,
        Long postId,
        Long commentId,
        Long requestId,
        String link,
        boolean allowSelf
) implements NotifiableEvent {

    public static SimpleNotifiableEvent of(
            Long receiverUserId,
            Long actorUserId,
            NotificationType type,
            String message,
            Long postId,
            Long commentId
    ) {
        return new SimpleNotifiableEvent(receiverUserId, actorUserId, type, message, postId, commentId, null, null, false);
    }

    public static SimpleNotifiableEvent of(
            Long receiverUserId,
            Long actorUserId,
            NotificationType type,
            String message,
            Long postId,
            Long commentId,
            Long requestId,
            String link
    ) {
        return new SimpleNotifiableEvent(receiverUserId, actorUserId, type, message, postId, commentId, requestId, link, false);
    }

    public static SimpleNotifiableEvent ofAllowSelf(
            Long receiverUserId,
            Long actorUserId,
            NotificationType type,
            String message,
            Long postId,
            Long commentId
    ){
        return new SimpleNotifiableEvent(receiverUserId, actorUserId, type, message, postId, commentId, null, null, true);
    }
}
