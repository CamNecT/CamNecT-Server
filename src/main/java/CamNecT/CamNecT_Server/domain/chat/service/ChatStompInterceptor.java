package CamNecT.CamNecT_Server.domain.chat.service;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
/*

@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {

    private final ChatPresenceService presenceService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        System.out.println("📨 COMMAND = " + accessor.getCommand());

        if (command == null) {
            return message;
        }

        */
/* =========================
 * 1️⃣ CONNECT
 * ========================= *//*

        if (command == StompCommand.CONNECT) {
            String userIdHeader = accessor.getFirstNativeHeader("userId");

            if (userIdHeader != null) {
                accessor.getSessionAttributes().put("userId", userIdHeader);
                System.out.println("🔐 CONNECT userId=" + userIdHeader);
            }

            return message;
        }

        */
/* =========================
 * 2️⃣ SUBSCRIBE
 * ========================= *//*

        if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/sub/chat/room/")) {

                Object userIdObj = accessor.getSessionAttributes().get("userId");
                if (userIdObj == null) {
                    return message;
                }

                Long userId = Long.valueOf(userIdObj.toString());
                Long roomId = extractRoomId(destination);

                // presence 등록
                presenceService.enter(roomId, userId);
                System.out.println("👤 SUBSCRIBE userId=" + userId + ", roomId=" + roomId);

                // 읽음 처리
                Users user = userRepository.getReferenceById(userId);
                chatService.markAllAsRead(roomId, user);
            }

            return message;
        }

        */
/* =========================
 * 3️⃣ DISCONNECT
 * ========================= *//*

        if (command == StompCommand.DISCONNECT) {

            Object userIdObj = accessor.getSessionAttributes().get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                presenceService.leaveAll(userId);
                System.out.println("❌ DISCONNECT userId=" + userId);
            }

            return message;
        }

        return message;
    }

    private Long extractRoomId(String destination) {
        return Long.parseLong(destination.substring(destination.lastIndexOf("/") + 1));
    }
}*/

@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            String userIdHeader = accessor.getFirstNativeHeader("userId");
            if (userIdHeader != null) {
                accessor.getSessionAttributes().put("userId", userIdHeader);
            }
        }

        // SUBSCRIBE, DISCONNECT 등등은 여기서 처리하지 않고 통과시킴
        return message;
    }
}