package CamNecT.CamNecT_Server.global.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationWsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),          // Principal name과 매칭
                "/queue/notifications",     // FE는 /user/queue/notifications 구독
                payload
        );
    }
}