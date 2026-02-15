package CamNecT.server.domain.chat.dto.room;

import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.users.model.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomListDetailDto {
    private Long roomId;
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

        Users opponent = room.getRequester().getUserId().equals(me.getUserId())
                ? room.getReceiver() : room.getRequester();

        return ChatRoomListDetailDto.builder()
                .roomId(room.getId())
                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())
                .opponentProfileImgUrl(profileImgUrl)

                .opponentMajor(major)
                .opponentStudentYear(studentYear)
                .lastMessage(lastMessage)
                .lastMessageTime(room.getUpdatedAt().toString())

                .unreadCount(count)
                .build();
    }
}