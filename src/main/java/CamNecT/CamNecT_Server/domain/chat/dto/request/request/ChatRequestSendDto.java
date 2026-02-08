package CamNecT.CamNecT_Server.domain.chat.dto.request.request;

import java.util.List;

public record ChatRequestSendDto(
        Long receiverId,
        List<Long> tagIds,
        String content
) {
}
