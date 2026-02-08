package CamNecT.CamNecT_Server.domain.chat.dto.request;

public record ChatRequestAcceptDto(
        Long requestId,
//        Long userId,
        boolean isAccepted
) {
}
