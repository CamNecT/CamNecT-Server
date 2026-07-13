package CamNecT.server.domain.chat.controller;

import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.service.ChatPresenceService;
import CamNecT.server.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;

    @MessageMapping("/chat/message")
    public void send(@Valid ChatMessageSendRequestDto dto, SimpMessageHeaderAccessor accessor) {
        Long senderId = accessor.getSessionAttributes() == null
                ? null
                : (Long) accessor.getSessionAttributes().get("userId");

        if (senderId == null) {
            log.error("세션에 userId가 없습니다. (헤더 토큰 누락 또는 만료)");
            return;
        }
        chatService.sendMessage(senderId, dto);
    }

    @MessageMapping("/chat/room/{roomId}/leave")
    public void leave(@DestinationVariable Long roomId, SimpMessageHeaderAccessor accessor) {
        Long userId = accessor.getSessionAttributes() == null
                ? null
                : (Long) accessor.getSessionAttributes().get("userId");

        if (userId == null) {
            log.warn("채팅방 퇴장 요청에 인증 사용자 정보가 없습니다. roomId={}", roomId);
            return;
        }

        presenceService.leaveRoom(roomId, userId, accessor.getSessionId());

        log.info("👋 LEAVE (방 퇴장): userId={}, roomId={}", userId, roomId);
    }
}
