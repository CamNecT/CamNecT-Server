package CamNecT.server.domain.chat.dto.message;

public enum ChatSocketOperation {
    CONNECT,
    SUBSCRIBE,
    SEND_MESSAGE,
    LEAVE_ROOM,
    UNKNOWN
}
