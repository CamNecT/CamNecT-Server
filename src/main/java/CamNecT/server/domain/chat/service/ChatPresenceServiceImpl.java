package CamNecT.server.domain.chat.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceServiceImpl implements ChatPresenceService {

    private final Map<Long, Set<Long>> roomUsers = new ConcurrentHashMap<>();

    @Override
    public void enter(Long roomId, Long userId) {
        roomUsers
                .computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);
        System.out.println("🟢 ENTER room=" + roomId + ", user=" + userId);
    }

    @Override
    public void leave(Long roomId, Long userId) {
        Set<Long> users = roomUsers.get(roomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                roomUsers.remove(roomId);
            }
        }

        System.out.println("🔴 LEAVE room=" + roomId + ", user=" + userId);
    }

    @Override
    public void leaveAll(Long userId) {
        roomUsers.values().forEach(set -> set.remove(userId));
        System.out.println("❌ LEAVE ALL user=" + userId);
    }

    @Override
    public boolean isPresent(Long roomId, Long userId) {
        boolean present =
                roomUsers.getOrDefault(roomId, Set.of()).contains(userId);

        System.out.println("👀 CHECK room=" + roomId + ", user=" + userId + " => " + present);
        return present;
    }
}
