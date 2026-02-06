package CamNecT.CamNecT_Server.domain.chat.service;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatPresenceService presenceService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/sub/chat/room/")) {
            Object userIdObj = accessor.getSessionAttributes().get("userId");
            if (userIdObj == null) return;

            Long userId = Long.valueOf(userIdObj.toString());
            Long roomId = extractRoomId(destination);

            presenceService.enter(roomId, userId);
            log.info("👤 SUBSCRIBE (입장): userId={}, roomId={}", userId, roomId);

            try {
                Users user = userRepository.getReferenceById(userId);
                chatService.markAllAsRead(roomId, user);
            } catch (Exception e) {
                log.error("읽음 처리 중 오류 발생: {}", e.getMessage());
            }
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (userId != null) {
            presenceService.leaveAll(userId);
            log.info("❌ DISCONNECT (퇴장): userId={}", userId);
        }
    }

    private Long extractRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf("/") + 1));
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Room ID 추출 실패: destination={}", destination);
            return null;
        }
    }
}