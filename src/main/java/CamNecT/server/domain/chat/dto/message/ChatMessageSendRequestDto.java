package CamNecT.server.domain.chat.dto.message;


public record ChatMessageSendRequestDto(
        Long roomId,
        String content
) {
}