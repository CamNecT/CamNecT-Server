package CamNecT.CamNecT_Server.domain.chat.service;

import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import CamNecT.CamNecT_Server.domain.chat.service.ChatPresenceService;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

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
            System.out.println("👤 SUBSCRIBE(Listener) userId=" + userId + ", roomId=" + roomId);

            Users user = userRepository.getReferenceById(userId);
            chatService.markAllAsRead(roomId, user);
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Object userIdObj = accessor.getSessionAttributes().get("userId");

        if (userIdObj != null) {
            Long userId = Long.valueOf(userIdObj.toString());
            presenceService.leaveAll(userId);
            System.out.println("❌ DISCONNECT(Listener) userId=" + userId);
        }
    }

    private Long extractRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf("/") + 1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}