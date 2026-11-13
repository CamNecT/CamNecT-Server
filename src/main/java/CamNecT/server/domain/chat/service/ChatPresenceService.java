package CamNecT.server.domain.chat.service;

public interface ChatPresenceService {
    void enter(Long roomId, Long userId, String sessionId, String subscriptionId,
               String loginSessionId, Long firstUnreadMessageId);
    Long takeFirstUnreadMessageId(Long roomId, Long userId, String loginSessionId);
    void leaveSubscription(String sessionId, String subscriptionId);
    void leaveRoom(Long roomId, Long userId, String sessionId);
    void leaveSession(String sessionId);
    boolean isPresent(Long roomId, Long userId);
}
