package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.model.NotificationType;
import CamNecT.CamNecT_Server.global.notification.service.FCMSender;
import CamNecT.CamNecT_Server.global.notification.service.NotificationService;
import CamNecT.CamNecT_Server.global.notification.service.PushDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FCMSender fcmSender;
    private final PushDeviceService pushDeviceService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotifiableEvent e) {

        // 0) self 알림 차단(기본)
        if (!e.allowSelf()
                && e.actorUserId() != null
                && e.receiverUserId().equals(e.actorUserId())) {
            return;
        }

        // 1) DB 저장
        notificationService.create(
                e.receiverUserId(),
                e.actorUserId(),
                e.type(),
                e.message(),
                e.postId(),
                e.commentId(),
                e.requestId(),
                e.link()
        );

        // 2) 푸시 발송(토큰 없으면 스킵)
        var tokens = pushDeviceService.findEnabledTokens(e.receiverUserId());
        if (tokens == null || tokens.isEmpty()) return;

        String title = titleOf(e.type());
        String body = e.message();

        // 3) data payload (FE에서 라우팅/처리에 사용)
        Map<String, String> data = new java.util.HashMap<>();
        data.put("type", e.type().name());
        if (e.postId() != null) data.put("postId", String.valueOf(e.postId()));
        if (e.commentId() != null) data.put("commentId", String.valueOf(e.commentId()));
        if (e.requestId() != null) data.put("requestId", String.valueOf(e.requestId()));
        if (e.link() != null) data.put("link", e.link());

        try {
            FCMSender.SendResult result = fcmSender.sendToTokens(tokens, title, body, data);
            // 무효 토큰 비활성화
            pushDeviceService.disableTokens(result.invalidTokens());
        } catch (com.google.firebase.messaging.FirebaseMessagingException ex) {
            // 커밋 이후이므로 비즈니스는 성공. 푸시 실패는 로깅만.
            // log.warn("FCM send failed: receiver={}, type={}", e.receiverUserId(), e.type(), ex);
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