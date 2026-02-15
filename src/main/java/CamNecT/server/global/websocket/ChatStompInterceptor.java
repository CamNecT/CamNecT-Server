package CamNecT.server.global.websocket;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = extractBearer(accessor);

            try {
                jwtUtil.validateOrThrow(token);

                Long userId = jwtUtil.getUserId(token);
                accessor.getSessionAttributes().put("userId", userId);

                accessor.setUser(new StompPrincipal(userId.toString()));

                log.info("소켓 연결 성공: userId = {}", userId);

            } catch (CustomException e) {
                // 토큰 검증 실패 시 소켓 연결 거부
                log.error("소켓 인증 실패: {}", e.getMessage());
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }
        }

        return message;
    }

    private String extractBearer(StompHeaderAccessor accessor) {
        String rawToken = accessor.getFirstNativeHeader("Authorization");

        if (rawToken == null || !rawToken.startsWith("Bearer ")) {
            throw new CustomException(AuthErrorCode.ACCESS_TOKEN_REQUIRED);
        }

        return rawToken.substring(7);
    }
}