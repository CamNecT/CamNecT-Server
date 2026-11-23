package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.dto.room.ChatRoomListUpdateDto;
import CamNecT.server.domain.chat.event.ChatMessageCommittedEvent;
import CamNecT.server.domain.chat.event.ChatReadCommittedEvent;
import CamNecT.server.domain.chat.event.ChatRoomClosedCommittedEvent;
import CamNecT.server.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRealtimeDeliveryService {

    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void deliverMessage(ChatMessageCommittedEvent event) {
        Long roomId = event.message().getRoomId();
        sendSafely("/sub/chat/room/" + roomId, event.message(), "message-room");

        try {
            var counts = chatRepository.countForDelivery(roomId, List.of(event.receiverId(), event.senderId())).stream()
                    .collect(Collectors.toMap(ChatRepository.RecipientUnread::getUserId, row -> row));
            ChatRoomListUpdateDto receiverUpdate = roomListUpdate(event, counts.get(event.receiverId()));
            ChatRoomListUpdateDto senderUpdate = roomListUpdate(event, counts.get(event.senderId()));

            sendSafely("/sub/user/" + event.receiverId() + "/rooms",
                    receiverUpdate, "message-receiver-list");
            sendSafely("/sub/user/" + event.senderId() + "/rooms",
                    senderUpdate, "message-sender-list");
        } catch (Exception e) {
            log.warn("[chat-realtime] room-list payload query failed. roomId={}", roomId, e);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void deliverRead(ChatReadCommittedEvent event) {
        Long roomId = event.readEvent().getRoomId();
        sendSafely("/sub/chat/room/" + roomId, event.readEvent(), "read-room");

        try {
            long totalUnread = chatRepository.countVisibleUnreadByUserId(event.readerId());
            ChatRoomListUpdateDto readerUpdate = ChatRoomListUpdateDto.builder()
                    .roomId(roomId)
                    .lastMessage(event.lastMessage())
                    .unreadCount(0L)
                    .time(event.lastMessageTime())
                    .totalUnreadCount(totalUnread)
                    .build();

            sendSafely("/sub/user/" + event.readerId() + "/rooms",
                    readerUpdate, "read-reader-list");
        } catch (Exception e) {
            log.warn("[chat-realtime] read room-list payload query failed. roomId={}, reader={}",
                    roomId, event.readerId(), e);
        }
    }

    public void deliverRoomClosed(ChatRoomClosedCommittedEvent event) {
        Long roomId = event.closedEvent().roomId();
        sendSafely("/sub/chat/room/" + roomId, event.closedEvent(), "room-close");
    }

    private ChatRoomListUpdateDto roomListUpdate(
            ChatMessageCommittedEvent event,
            ChatRepository.RecipientUnread counts
    ) {
        return ChatRoomListUpdateDto.builder()
                .roomId(event.message().getRoomId())
                .lastMessage(event.lastMessage())
                .unreadCount(counts == null ? 0L : counts.getRoomUnread())
                .time(event.lastMessageTime())
                .totalUnreadCount(counts == null ? 0L : counts.getTotalUnread())
                .build();
    }

    private void sendSafely(String destination, Object payload, String operation) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.warn("[chat-realtime] delivery failed. operation={}, destination={}",
                    operation, destination, e);
        }
    }
}
