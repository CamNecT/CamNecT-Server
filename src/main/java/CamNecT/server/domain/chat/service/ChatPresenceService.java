package CamNecT.server.domain.chat.service;

public interface ChatPresenceService {
    void enter(Long roomId, Long userId, String sessionId, String subscriptionId);
    void leaveSubscription(String sessionId, String subscriptionId);
    void leaveRoom(Long roomId, Long userId, String sessionId);
    void leaveSession(String sessionId);
    boolean isPresent(Long roomId, Long userId);
}
