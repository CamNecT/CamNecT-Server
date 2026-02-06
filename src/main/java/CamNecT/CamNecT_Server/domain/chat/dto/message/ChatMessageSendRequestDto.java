package CamNecT.CamNecT_Server.domain.chat.dto.message;


public record ChatMessageSendRequestDto(
        Long roomId,
//        Long senderId, //-> 보통 SecurityContext(토큰)에서 꺼내서 안전을 위해 X
        String content
) {
}