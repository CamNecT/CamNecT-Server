package CamNecT.CamNecT_Server.global.common.config;

import CamNecT.CamNecT_Server.domain.chat.service.ChatStompInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatStompInterceptor chatStompInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue");    //해당 주소를 구독하고 잇는 클라이언트들에게 메세지 전달
        registry.setApplicationDestinationPrefixes("/pub");       //클라이언트에서 보낸 메세지를 받을 prefix
        registry.setApplicationDestinationPrefixes("/send");
        registry.enableSimpleBroker("/room");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns(
                        "https://camnect.site",
                        "https://www.camnect.site",
                        "https://camnect-web.vercel.app",
                        "http://localhost:5173"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatStompInterceptor);
    }
}
