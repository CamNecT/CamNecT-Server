package CamNecT.server.domain.chat.dto.request.request;

public record ChatRequestAcceptDto(
        Long requestId,
        boolean isAccepted
) {
}
