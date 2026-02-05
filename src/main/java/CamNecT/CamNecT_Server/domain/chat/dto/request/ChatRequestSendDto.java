package CamNecT.CamNecT_Server.domain.chat.dto.request;

import java.util.List;

public record ChatRequestSendDto(
        Long requesterId, //테스트용- 토큰 오면 지움
        Long receiverId,
        List<Long> tagIds,
        String content
) {
}
