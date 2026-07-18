package CamNecT.server.domain.chat.dto.message;

import CamNecT.server.domain.chat.model.Chat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {

    private Long messageId;
    private Long roomId;
    private String clientMessageId;

    private Long senderId;
    private String sender;

    private Long receiverId;
    private String receiver;

    private String message;

    private boolean read;

    private String sendDate;
    private String readAt;

    public static ChatMessageResponseDto toDto(Chat chat) {
        return ChatMessageResponseDto.builder()
                .messageId(chat.getId())
                .roomId(chat.getRoom().getId())
                .clientMessageId(chat.getClientMessageId())

                .senderId(chat.getSender().getUserId())
                .sender(chat.getSender().getName())

                .receiverId(chat.getReceiver().getUserId())
                 .receiver(chat.getReceiver().getName())
                .message(chat.getContent())
                .read(chat.isRead())

                .readAt(chat.getReadAt() != null ? chat.getReadAt().toString() : null)
                .sendDate(chat.getCreatedAt().toString())
                .build();
    }
}
