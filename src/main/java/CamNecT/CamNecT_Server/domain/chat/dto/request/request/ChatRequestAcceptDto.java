package CamNecT.CamNecT_Server.domain.chat.dto.request.request;

public record ChatRequestAcceptDto(
        Long requestId,
        boolean isAccepted
) {
}
