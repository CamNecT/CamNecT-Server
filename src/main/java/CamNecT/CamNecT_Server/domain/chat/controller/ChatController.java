package CamNecT.CamNecT_Server.domain.chat.controller;

import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.CamNecT_Server.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

/*    @MessageMapping("/{roomId}")
    @SendTo("/room/{roomId}")
    public ChatMessageDto chat(@DestinationVariable Long roomId, ChatMessageDto messageDto) {

        Chat savedChat = chatService.sendMessage(roomId, messageDto.getSenderId(), messageDto.getMessage());

        return ChatMessageDto.toDto(savedChat);
    }*/

    // 토큰 적용 전 - 로컬 테스트용 send
    @MessageMapping("/chat/message")
    public void send(ChatMessageSendRequestDto dto) {
        chatService.sendMessage(dto);
    }

/*    @MessageMapping("/chat/message")
    public void send(@UserId Long senderId, ChatMessageSendRequestDto request) {
        chatService.sendMessage(request, senderId);
    }*/
}