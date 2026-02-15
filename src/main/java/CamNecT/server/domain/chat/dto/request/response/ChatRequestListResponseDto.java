package CamNecT.server.domain.chat.dto.request.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRequestListResponseDto(
        List<ChatRequestListDetailDto> chatRequestList
) {
}
