package CamNecT.server.domain.chat.controller;

import CamNecT.server.domain.chat.dto.message.ChatMessageAckResponseDto;
import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.service.ChatPresenceService;
import CamNecT.server.domain.chat.service.ChatService;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.websocket.ChatSocketErrorMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerContractTest {

    private final ChatService chatService = mock(ChatService.class);
    private final ChatPresenceService presenceService = mock(ChatPresenceService.class);
    private final ChatSocketErrorMapper errorMapper = mock(ChatSocketErrorMapper.class);
    private final ChatController controller = new ChatController(chatService, presenceService, errorMapper);

    @Test
    void sendReturnsSessionScopedAckAfterServiceCompletes() throws Exception {
        ChatMessageSendRequestDto request = new ChatMessageSendRequestDto(
                99L, "hello", "0e9e31aa-99e7-4c58-90d8-f939b56fd234");
        ChatMessageAckResponseDto expected = new ChatMessageAckResponseDto(
                "ACK", 10L, 99L, request.clientMessageId(), false);
        when(chatService.sendMessage(1L, request)).thenReturn(expected);

        ChatMessageAckResponseDto actual = controller.send(request, accessor(1L));

        assertThat(actual).isEqualTo(expected);
        verify(chatService).sendMessage(1L, request);

        Method method = ChatController.class.getMethod(
                "send", ChatMessageSendRequestDto.class, SimpMessageHeaderAccessor.class);
        SendToUser sendToUser = method.getAnnotation(SendToUser.class);
        assertThat(sendToUser.value()).containsExactly("/queue/chat-acks");
        assertThat(sendToUser.broadcast()).isFalse();
    }

    @Test
    void missingAuthenticatedSessionIsReportedAsInvalidToken() {
        CustomException exception = assertThrows(CustomException.class,
                () -> controller.send(
                        new ChatMessageSendRequestDto(99L, "hello", null),
                        accessor(null)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    private SimpMessageHeaderAccessor accessor(Long userId) {
        SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Map<String, Object> attributes = new HashMap<>();
        if (userId != null) attributes.put("userId", userId);
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}
