package CamNecT.server.domain.chat.service;

public interface ChatPresenceService {
    void enter(Long roomId, Long userId, String sessionId, String subscriptionId,
               String loginSessionId, Long firstUnreadMessageId);
    Long firstUnreadForEntry(Long roomId, Long userId, String loginSessionId, Long firstUnreadOnGet);
    void leaveSubscription(String sessionId, String subscriptionId);
    void leaveRoom(Long roomId, Long userId, String sessionId, String loginSessionId);
    void leaveSession(String sessionId);
    boolean isPresent(Long roomId, Long userId);
}
