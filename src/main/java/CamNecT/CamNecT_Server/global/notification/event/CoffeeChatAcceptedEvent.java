package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.model.NotificationType;

public record CoffeeChatAcceptedEvent(
        Long receiverUserId,
        Long actorUserId,
        Long roomId
) implements NotifiableEvent {
    @Override public NotificationType type() { return NotificationType.COFFEE_CHAT_ACCEPTED; }
    @Override public String message() { return "커피챗이 승인되었습니다."; }
    @Override public Long roomId() { return roomId; }
}