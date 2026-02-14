package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.model.NotificationType;

public interface NotifiableEvent {
    Long receiverUserId();
    Long actorUserId();
    NotificationType type();
    String message();

    default Long postId() { return null; }
    default Long commentId() { return null; }

    // 커피챗/기타 식별자용
    default Long requestId() { return null; }
    default Long roomId() { return null; }

    // FE 딥링크(선택)
    default String link() { return null; }

    // “나->나” 알림 허용 여부(기본 false)
    default boolean allowSelf() { return false; }
}