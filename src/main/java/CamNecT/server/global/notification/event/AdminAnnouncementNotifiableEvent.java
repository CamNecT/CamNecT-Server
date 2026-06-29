package CamNecT.server.global.notification.event;

import CamNecT.server.global.notification.model.NotificationType;

public record AdminAnnouncementNotifiableEvent(
        Long receiverUserId,
        Long actorUserId,
        String message,
        String link
) implements NotifiableEvent {

    @Override
    public NotificationType type() {
        return NotificationType.ADMIN_ANNOUNCEMENT;
    }

    @Override
    public Long postId() {
        return null;
    }

    @Override
    public Long commentId() {
        return null;
    }

    @Override
    public Long requestId() {
        return null;
    }
}