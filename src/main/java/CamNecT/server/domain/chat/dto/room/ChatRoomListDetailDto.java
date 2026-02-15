package CamNecT.server.domain.chat.dto.room;

import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.users.model.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomListDetailDto {
    private Long roomId;
    private boolean isClosed;
    private boolean isOpponentExited;

    private Long opponentId;
    private String opponentName;
    private String opponentProfileImgUrl;

    private String opponentMajor;
    private String opponentStudentYear;
    private String lastMessage;

    private String lastMessageTime;

    private Long unreadCount; // 방별 안읽은 수

    public static ChatRoomListDetailDto of(ChatRoom room, Users me, Long count,
                                           String major, String studentYear, String lastMessage, String profileImgUrl) {

        boolean isMeRequester = room.getRequester().getUserId().equals(me.getUserId());
        Users opponent = room.getRequester().getUserId().equals(me.getUserId())
                ? room.getReceiver() : room.getRequester();

        boolean opponentExited = isMeRequester ? room.isReceiverExited() : room.isRequesterExited();

        return ChatRoomListDetailDto.builder()
                .roomId(room.getId())
                .isClosed(room.getStatus().equals(ChatRoom.RoomStatus.CLOSE))
                .isOpponentExited(opponentExited)
                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())
                .opponentProfileImgUrl(profileImgUrl)

                .opponentMajor(major)
                .opponentStudentYear(studentYear)
                .lastMessage(lastMessage)
                .lastMessageTime(room.getLastMessageAt() != null ? room.getLastMessageAt().toString() : room.getCreatedAt().toString())

                .unreadCount(count)
                .build();
    }
}