package CamNecT.server.domain.chat.dto.room;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomListResponseDto(
        List<ChatRoomListDetailDto> chatRoomList,
        Long totalUnreadCount
        // todo : 리퀘스트 왔으면 true 타입으로 Boolean
) {
}