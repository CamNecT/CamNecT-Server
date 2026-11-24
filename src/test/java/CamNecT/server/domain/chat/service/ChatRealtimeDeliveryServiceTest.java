package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.server.domain.chat.dto.message.ChatReadEvent;
import CamNecT.server.domain.chat.dto.room.ChatRoomListUpdateDto;
import CamNecT.server.domain.chat.event.ChatMessageCommittedEvent;
import CamNecT.server.domain.chat.event.ChatReadCommittedEvent;
import CamNecT.server.domain.chat.event.ChatRoomClosedCommittedEvent;
import CamNecT.server.domain.chat.repository.ChatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRealtimeDeliveryServiceTest {

    @Mock ChatRepository chatRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks ChatRealtimeDeliveryService deliveryService;

    @Test
    void messageDeliveryUpdatesRoomAndBothRoomLists() {
        ChatMessageCommittedEvent event = messageEvent();
        var sender = mock(ChatRepository.RecipientUnread.class);
        when(sender.getUserId()).thenReturn(1L);
        when(sender.getRoomUnread()).thenReturn(3L);
        when(sender.getTotalUnread()).thenReturn(5L);
        when(chatRepository.countForDelivery(99L, List.of(2L, 1L))).thenReturn(List.of(sender));

        deliveryService.deliverMessage(event);

        verify(messagingTemplate).convertAndSend("/sub/chat/room/99", event.message());
        var receiverUpdate = ArgumentCaptor.forClass(ChatRoomListUpdateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/user/2/rooms"), receiverUpdate.capture());
        assertThat(receiverUpdate.getValue().getTotalUnreadCount()).isZero();
        ArgumentCaptor<ChatRoomListUpdateDto> senderUpdate =
                ArgumentCaptor.forClass(ChatRoomListUpdateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/user/1/rooms"), senderUpdate.capture());
        assertThat(senderUpdate.getValue().getUnreadCount()).isEqualTo(3L);
        assertThat(senderUpdate.getValue().getTotalUnreadCount()).isEqualTo(5L);
    }

    @Test
    void oneSocketFailureDoesNotStopOtherDestinations() {
        ChatMessageCommittedEvent event = messageEvent();
        doThrow(new IllegalStateException("room socket down"))
                .when(messagingTemplate).convertAndSend("/sub/chat/room/99", event.message());

        deliveryService.deliverMessage(event);

        verify(messagingTemplate).convertAndSend(eq("/sub/user/2/rooms"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/sub/user/1/rooms"), any(Object.class));
    }

    @Test
    void readDeliveryUsesVisibleUnreadCountAndUpdatesRoomAndReaderList() {
        ChatReadEvent readEvent = ChatReadEvent.of(99L, 20L, "2026-01-01T10:01:00");
        ChatReadCommittedEvent event = new ChatReadCommittedEvent(
                readEvent, 2L, "last", "2026-01-01T10:00:00");
        when(chatRepository.countVisibleUnreadByUserId(2L)).thenReturn(3L);

        deliveryService.deliverRead(event);

        verify(messagingTemplate).convertAndSend("/sub/chat/room/99", readEvent);
        verify(messagingTemplate).convertAndSend(eq("/sub/user/2/rooms"), any(Object.class));
        verify(chatRepository).countVisibleUnreadByUserId(2L);
    }

    @Test
    void roomClosedDeliveryUsesRoomSubscriptionAndExactPayload() {
        ChatRoomClosedCommittedEvent event = new ChatRoomClosedCommittedEvent(99L);

        deliveryService.deliverRoomClosed(event);

        verify(messagingTemplate).convertAndSend(
                "/sub/chat/room/99", event.closedEvent());
        verifyNoInteractions(chatRepository);
    }

    private ChatMessageCommittedEvent messageEvent() {
        ChatMessageResponseDto response = ChatMessageResponseDto.builder()
                .messageId(10L)
                .roomId(99L)
                .senderId(1L)
                .receiverId(2L)
                .message("hello")
                .sendDate("2026-01-01T10:00:00")
                .build();
        return new ChatMessageCommittedEvent(
                response, 1L, 2L, "hello", "2026-01-01T10:00:00");
    }
}
