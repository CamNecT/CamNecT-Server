package CamNecT.server.global.websocket;

import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.server.global.jwt.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStompInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AccountAccessGuard accountAccessGuard = mock(AccountAccessGuard.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatStompInterceptor interceptor =
            new ChatStompInterceptor(jwtUtil, accountAccessGuard, chatRoomRepository);

    @Test
    void rejectsSubscriptionToRoomWhereUserIsNotParticipant() {
        when(chatRoomRepository.existsAccessibleByUserId(99L, 1L)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> interceptor.preSend(subscribe("/sub/chat/room/99", 1L), null));

        assertThat(ex.getErrorCode()).isEqualTo(CoffeeChatErrorCode.CHATROOM_ACCESS_DENIED);
    }

    @Test
    void rejectsAnotherUsersRoomListSubscription() {
        CustomException ex = assertThrows(CustomException.class,
                () -> interceptor.preSend(subscribe("/sub/user/2/rooms", 1L), null));

        assertThat(ex.getErrorCode()).isEqualTo(CoffeeChatErrorCode.CHATROOM_ACCESS_DENIED);
    }

    @Test
    void allowsOwnAccessibleSubscriptions() {
        when(chatRoomRepository.existsAccessibleByUserId(99L, 1L)).thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(subscribe("/sub/chat/room/99", 1L), null));
        assertDoesNotThrow(() -> interceptor.preSend(subscribe("/sub/user/1/rooms", 1L), null));
        assertDoesNotThrow(() -> interceptor.preSend(subscribe("/user/queue/chat-errors", 1L), null));
        assertDoesNotThrow(() -> interceptor.preSend(subscribe("/user/queue/chat-acks", 1L), null));
    }

    @Test
    void rejectsUnknownSubscriptionDestination() {
        CustomException ex = assertThrows(CustomException.class,
                () -> interceptor.preSend(subscribe("/sub/admin/secret", 1L), null));

        assertThat(ex.getErrorCode()).isEqualTo(CoffeeChatErrorCode.CHATROOM_ACCESS_DENIED);
    }

    private Message<byte[]> subscribe(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(new HashMap<>(Map.of("userId", userId)));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
