package CamNecT.server.global.notification.event;

import CamNecT.server.global.notification.model.NotificationType;

public record CoffeeChatRequestedEvent(
        Long receiverUserId,
        Long actorUserId,
        Long requestId
) implements NotifiableEvent {

    @Override public NotificationType type() { return NotificationType.COFFEE_CHAT_REQUESTED; }
    @Override public String message() { return "커피챗 요청이 도착했습니다."; }
    @Override public Long requestId() { return requestId; }
    @Override public String link() { return null; } //추후 수정
}