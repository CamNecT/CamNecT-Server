package CamNecT.server.global.websocket;

import CamNecT.server.domain.chat.dto.message.ChatSocketErrorResponse;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStompErrorHandler extends StompSubProtocolErrorHandler {

    private final ChatSocketErrorMapper errorMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected Message<byte[]> handleInternal(
            StompHeaderAccessor errorHeaderAccessor,
            byte[] errorPayload,
            Throwable cause,
            StompHeaderAccessor clientHeaderAccessor
    ) {
        ChatSocketErrorResponse response = errorMapper.map(cause, clientHeaderAccessor, null);
        byte[] payload = serialize(response);

        errorHeaderAccessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        errorHeaderAccessor.setContentLength(payload.length);
        errorHeaderAccessor.setMessage(response.message());

        log.warn("[chat-stomp-error] operation={} status={} code={} roomId={}",
                response.operation(), response.status(), response.code(), response.roomId());
        return MessageBuilder.createMessage(payload, errorHeaderAccessor.getMessageHeaders());
    }

    private byte[] serialize(ChatSocketErrorResponse response) {
        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (Exception e) {
            log.error("[chat-stomp-error] failed to serialize structured error", e);
            return ("{\"type\":\"ERROR\",\"status\":500,\"code\":"
                    + ErrorCode.INTERNAL_ERROR.getCode()
                    + ",\"message\":\"서버 내부 오류가 발생했습니다.\","
                    + "\"operation\":\"UNKNOWN\",\"roomId\":null,\"clientMessageId\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}
