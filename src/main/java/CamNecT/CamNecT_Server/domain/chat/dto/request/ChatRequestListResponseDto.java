package CamNecT.CamNecT_Server.domain.chat.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRequestListResponseDto(
        List<ChatRequestListDetailDto> chatRequestList
) {
}
