package CamNecT.server.domain.chat.dto.room;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomListResponseDto(
        List<ChatRoomListDetailDto> chatRoomList,
        Long totalUnreadCount,
        boolean requestExists
) {
}