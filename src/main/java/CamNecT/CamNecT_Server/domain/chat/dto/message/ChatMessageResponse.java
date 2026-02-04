package CamNecT.CamNecT_Server.domain.chat.dto.message;

import CamNecT.CamNecT_Server.domain.chat.model.Chat;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long roomId;

    private Long senderId;
    private String sender;

    private Long receiverId;
    private String receiver;

    private String message;
    private boolean isRead;
    private String readAt;

    private String sendDate;

    public static ChatMessageResponse from(Chat chat) {
        return ChatMessageResponse.builder()
                .messageId(chat.getId())
                .roomId(chat.getRoom().getId())
                .senderId(chat.getSender().getUserId())
                .sender(chat.getSender().getName())
                .receiverId(chat.getReceiver().getUserId())
                .receiver(chat.getReceiver().getName())
                .message(chat.getContent())
                .isRead(chat.isRead())
                .readAt(chat.getReadAt() == null ? null : chat.getReadAt().toString())
                .sendDate(chat.getCreatedAt() == null ? null : chat.getCreatedAt().toString())
                .build();
    }

}
