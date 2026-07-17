package CamNecT.server.global.websocket;

import CamNecT.server.domain.chat.dto.message.ChatSocketErrorResponse;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStompErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatStompErrorHandler errorHandler = new ChatStompErrorHandler(
            new ChatSocketErrorMapper(objectMapper),
            objectMapper
    );

    @Test
    void serializesConnectFailureAsStructuredStompErrorFrame() throws Exception {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setReceipt("connect-1");
        Message<byte[]> connect = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<byte[]> error = errorHandler.handleClientMessageProcessingError(
                connect,
                new CustomException(AuthErrorCode.INVALID_TOKEN)
        );

        StompHeaderAccessor errorAccessor = StompHeaderAccessor.wrap(error);
        ChatSocketErrorResponse payload = objectMapper.readValue(
                error.getPayload(), ChatSocketErrorResponse.class);

        assertThat(errorAccessor.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(errorAccessor.getReceiptId()).isEqualTo("connect-1");
        assertThat(payload.type()).isEqualTo("ERROR");
        assertThat(payload.status()).isEqualTo(401);
        assertThat(payload.operation()).isEqualTo("CONNECT");
    }

    @Test
    void includesRoomContextForRejectedSubscription() throws Exception {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/99");
        Message<byte[]> subscribe = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<byte[]> error = errorHandler.handleClientMessageProcessingError(
                subscribe,
                new CustomException(AuthErrorCode.INVALID_TOKEN)
        );
        ChatSocketErrorResponse payload = objectMapper.readValue(
                error.getPayload(), ChatSocketErrorResponse.class);

        assertThat(payload.operation()).isEqualTo("SUBSCRIBE");
        assertThat(payload.roomId()).isEqualTo(99L);
    }
}
