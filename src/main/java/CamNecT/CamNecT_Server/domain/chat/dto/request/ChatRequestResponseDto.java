package CamNecT.CamNecT_Server.domain.chat.dto.request;

public record ChatRequestResponseDto(
        Long requestId,
        Long userId,
        boolean isAccepted
) {
}
