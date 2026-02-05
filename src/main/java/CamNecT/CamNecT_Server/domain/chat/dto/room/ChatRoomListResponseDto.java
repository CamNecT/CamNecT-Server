package CamNecT.CamNecT_Server.domain.chat.dto.room;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomListResponseDto(
        List<ChatRoomListDetailDto> chatRoomList,
        Long totalUnreadCount
) {
}