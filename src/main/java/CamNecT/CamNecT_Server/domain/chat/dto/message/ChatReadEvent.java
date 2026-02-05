package CamNecT.CamNecT_Server.domain.chat.dto.message;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatReadEvent {
    private Long roomId;
    private Long lastReadMessageId;
    private String readAt;

    @Builder.Default
    private String type = "READ";

    public static ChatReadEvent of(Long roomId, Long lastReadMessageId, String readAt) {
        return ChatReadEvent.builder()
                .roomId(roomId)
                .lastReadMessageId(lastReadMessageId)
                .readAt(readAt)
                .type("READ")
                .build();
    }
}
