package CamNecT.server.domain.chat.dto.room;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomListUpdateDto {
    // 방 하나하나 갱신
    private Long roomId;
    private String lastMessage;
    private Long unreadCount;
    private String time;

    // 총 안읽은 수 갱신
    private Long totalUnreadCount;
}
