package CamNecT.CamNecT_Server.global.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

@Configuration
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    // todo: heartbeat 관련 설정 추가하기

//    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

/*        if (accessor.getCommand() == StompCommand.CONNECT) {
            validateToken(accessor);
        }*/

        return message;
    }

/*    private void validateToken(StompHeaderAccessor accessor) {
        String accessToken = jwtProvider.resolveToken(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));

        if (accessToken == null)
            throw new AccountException(StatusCode.FILTER_ACCESS_DENIED);

        jwtProvider.validate(accessToken);
    }*/

}