package CamNecT.server.domain.chat.dto.message;

public record ChatMessageAckResponseDto(
        String type,
        Long messageId,
        Long roomId,
        String clientMessageId,
        boolean duplicate
) {
    public static ChatMessageAckResponseDto from(ChatMessageResponseDto message, boolean duplicate) {
        return new ChatMessageAckResponseDto(
                "ACK",
                message.getMessageId(),
                message.getRoomId(),
                message.getClientMessageId(),
                duplicate
        );
    }
}
