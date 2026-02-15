package CamNecT.server.global.notification.debug;

import CamNecT.server.global.notification.event.NotifiableEvent;
import CamNecT.server.global.notification.model.NotificationType;

public record DebugNotifiableEvent(
        Long receiverUserId,
        Long actorUserId,
        Long roomId,
        String content
) implements NotifiableEvent {

    @Override public NotificationType type() { return NotificationType.CHAT_MESSAGE_RECEIVED; }

    @Override public String message() { return content; }

    @Override public Long requestId() { return roomId; }

    @Override public String link() { return "/chat/room/" + roomId; }
}