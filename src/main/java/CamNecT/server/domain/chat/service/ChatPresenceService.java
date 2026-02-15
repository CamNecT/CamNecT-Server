package CamNecT.server.domain.chat.service;

public interface ChatPresenceService {
    void enter(Long roomId, Long userId);
    void leave(Long roomId, Long userId);
    void leaveAll(Long userId);
    boolean isPresent(Long roomId, Long userId);
}
