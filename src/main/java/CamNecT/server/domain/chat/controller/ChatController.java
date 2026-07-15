package CamNecT.server.domain.chat.controller;

import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.dto.message.ChatSocketErrorResponse;
import CamNecT.server.domain.chat.service.ChatPresenceService;
import CamNecT.server.domain.chat.service.ChatService;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.websocket.ChatSocketErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;
    private final ChatSocketErrorMapper errorMapper;

    @MessageMapping("/chat/message")
    public void send(
            @Valid ChatMessageSendRequestDto dto,
            SimpMessageHeaderAccessor accessor
    ) {
        Long senderId = accessor.getSessionAttributes() == null
                ? null
                : (Long) accessor.getSessionAttributes().get("userId");

        if (senderId == null) {
            log.error("세션에 userId가 없습니다. (헤더 토큰 누락 또는 만료)");
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
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
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        presenceService.leaveRoom(roomId, userId, accessor.getSessionId());

        log.info("👋 LEAVE (방 퇴장): userId={}, roomId={}", userId, roomId);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(value = "/queue/chat-errors", broadcast = false)
    public ChatSocketErrorResponse handleChatException(Exception exception, Message<?> message) {
        ChatSocketErrorResponse response = errorMapper.map(exception, message);
        if (response.status() >= 500) {
            log.error("[chat-socket] unexpected application error. operation={}, roomId={}",
                    response.operation(), response.roomId(), exception);
        } else {
            log.warn("[chat-socket] rejected. operation={}, status={}, code={}, roomId={}, clientMessageId={}",
                    response.operation(), response.status(), response.code(),
                    response.roomId(), response.clientMessageId());
        }
        return response;
    }
}
