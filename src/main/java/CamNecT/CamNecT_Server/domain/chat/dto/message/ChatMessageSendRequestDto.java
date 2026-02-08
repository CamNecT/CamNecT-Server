package CamNecT.CamNecT_Server.domain.chat.dto.message;


public record ChatMessageSendRequestDto(
        Long roomId,
        String content
) {
}