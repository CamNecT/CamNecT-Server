package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void send(ChatMessageSendRequestDto dto, SimpMessageHeaderAccessor accessor) {
        Long senderId = (Long) accessor.getSessionAttributes().get("userId");

        if (senderId == null) {
            log.error("세션에 userId가 없습니다. (헤더 토큰 누락 또는 만료)");
            return;
        }
        chatService.sendMessage(senderId, dto);
    }

}