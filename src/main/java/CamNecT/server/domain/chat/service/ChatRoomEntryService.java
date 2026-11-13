package CamNecT.server.domain.chat.service;

import CamNecT.server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.server.domain.users.model.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ChatRoomEntryService {
    private final ChatService chatService;
    private final ChatPresenceService presenceService;

    // Keep the database commit and its in-memory handoff ordered for concurrent SUBSCRIBE and GET.
    private final Object[] entryLocks = Stream.generate(Object::new).limit(256).toArray();

    public void subscribe(Long roomId, Users user, String loginSessionId, String socketId, String subscriptionId) {
        Long userId = user.getUserId();
        synchronized (entryLock(roomId, userId)) {
            Long firstUnread = chatService.markAllAsRead(roomId, user);
            presenceService.enter(roomId, userId, socketId, subscriptionId, loginSessionId, firstUnread);
        }
    }

    public ChatRoomWithDetailDto getRoomWithDetails(Long roomId, Long userId, String loginSessionId) {
        synchronized (entryLock(roomId, userId)) {
            ChatRoomWithDetailDto details = chatService.getRoomWithDetails(roomId, userId);
            Long firstUnread = presenceService.firstUnreadForEntry(roomId, userId, loginSessionId,
                    details.getFirstUnreadMessageId());
            return details.withFirstUnreadMessageId(firstUnread);
        }
    }

    private Object entryLock(Long roomId, Long userId) {
        return entryLocks[Math.floorMod(Objects.hash(roomId, userId), entryLocks.length)];
    }
}
