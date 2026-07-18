package CamNecT.server.global.websocket;

import CamNecT.server.domain.chat.dto.message.ChatSocketErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSocketErrorPublisherTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks ChatSocketErrorPublisher publisher;

    @Test
    void sendsErrorOnlyToOriginatingSession() {
        ChatSocketErrorResponse response = response();

        publisher.sendToSession(1L, "session-a", response);

        ArgumentCaptor<MessageHeaders> headersCaptor = ArgumentCaptor.forClass(MessageHeaders.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"), eq("/queue/chat-errors"), eq(response), headersCaptor.capture());
        assertThat(headersCaptor.getValue().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER))
                .isEqualTo("session-a");
    }

    @Test
    void socketFailureDoesNotEscapePublisher() {
        ChatSocketErrorResponse response = response();
        doThrow(new IllegalStateException("broker unavailable"))
                .when(messagingTemplate)
                .convertAndSendToUser(eq("1"), eq("/queue/chat-errors"), eq(response),
                        org.mockito.ArgumentMatchers.any(MessageHeaders.class));

        assertDoesNotThrow(() -> publisher.sendToSession(1L, "session-a", response));
    }

    private ChatSocketErrorResponse response() {
        return new ChatSocketErrorResponse(
                "ERROR", 500, 50000, "서버 내부 오류가 발생했습니다.",
                "SUBSCRIBE", 99L, null);
    }
}
