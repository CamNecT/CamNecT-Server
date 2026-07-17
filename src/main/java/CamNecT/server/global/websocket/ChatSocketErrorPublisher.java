package CamNecT.server.global.websocket;

import CamNecT.server.domain.chat.dto.message.ChatSocketErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSocketErrorPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToSession(Long userId, String sessionId, ChatSocketErrorResponse response) {
        if (userId == null || sessionId == null) {
            log.warn("[chat-socket-error] skip error delivery without user/session. operation={}, code={}",
                    response.operation(), response.code());
            return;
        }

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/chat-errors",
                    response,
                    headers.getMessageHeaders()
            );
        } catch (Exception e) {
            log.warn("[chat-socket-error] delivery failed. userId={}, sessionId={}, operation={}, code={}",
                    userId, sessionId, response.operation(), response.code(), e);
        }
    }
}
