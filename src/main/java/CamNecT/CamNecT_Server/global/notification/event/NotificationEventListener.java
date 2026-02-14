package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.dto.NotificationPushPayload;
import CamNecT.CamNecT_Server.global.notification.model.NotificationType;
import CamNecT.CamNecT_Server.global.notification.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FCMSender fcmSender;
    private final PushDeviceService pushDeviceService;
    private final NotificationWsPublisher notificationWsPublisher;
    private final NotificationLinkResolver notificationLinkResolver;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void persist(NotifiableEvent e) {

        log.info("[notif] persist(beforeCommit) receiver={}, actor={}, type={}",
                e.receiverUserId(), e.actorUserId(), e.type());

        if (e.shouldSkipSelfNotification()) return;
        String link = notificationLinkResolver.resolve(e);

        notificationService.create(
                e.receiverUserId(),
                e.actorUserId(),
                e.type(),
                e.message(),
                e.postId(),
                e.commentId(),
                e.requestId(),
                link
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void push(NotifiableEvent e) {

        log.info("[notif] push(afterCommit) receiver={}, actor={}, type={}",
                e.receiverUserId(), e.actorUserId(), e.type());

        if (e.shouldSkipSelfNotification()) return;

        String title = titleOf(e.type());
        String body = e.message();
        String link = notificationLinkResolver.resolve(e);

        // 1) 웹/로컬 실시간 알림 (WebSocket user queue)
        var wsPayload = new NotificationPushPayload(
                e.type().name(),
                title,
                body,
                e.postId(),
                e.commentId(),
                e.requestId(),
                link
        );
        notificationWsPublisher.sendToUser(e.receiverUserId(), wsPayload);

        // 2) 모바일 푸시(FCM)
        var tokens = pushDeviceService.findEnabledTokens(e.receiverUserId());
        if (tokens == null || tokens.isEmpty()) return;


        Map<String, String> data = new java.util.HashMap<>();
        data.put("type", e.type().name());
        if (e.postId() != null) data.put("postId", String.valueOf(e.postId()));
        if (e.commentId() != null) data.put("commentId", String.valueOf(e.commentId()));
        if (e.requestId() != null) data.put("requestId", String.valueOf(e.requestId()));

        data.put("link", link);

        try {
            FCMSender.SendResult result = fcmSender.sendToTokens(tokens, title, body, data);
            pushDeviceService.disableTokens(result.invalidTokens());
        } catch (com.google.firebase.messaging.FirebaseMessagingException ex) {
            log.warn("[notif] FCM send failed. receiver={}, type={}", e.receiverUserId(), e.type(), ex);
        }
    }

    private String titleOf(NotificationType type) {
        return switch (type) {
            case COFFEE_CHAT_REQUESTED -> "[커피챗 요청]";
            case COMMENT_ACCEPTED -> "[댓글 채택]";
            case POST_COMMENTED -> "[댓글]";
            case COMMENT_REPLIED -> "[답글]";
            case POINT_EARNED -> "[포인트 적립]";
            case POINT_SPENT -> "[포인트 사용]";
            case CHAT_MESSAGE_RECEIVED -> "[새 메시지]";
            default -> "[알림]";
        };
    }
}