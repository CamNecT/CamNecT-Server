package CamNecT.CamNecT_Server.domain.chat.dto.room;

import CamNecT.CamNecT_Server.domain.chat.model.ChatRoom;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomListDetailDto {
    private Long roomId;
    private String opponentName;
    private String opponentProfileImgUrl;

    private String opponentMajor;
    private String opponentStudentYear;
    private String lastMessage;

    private Long unreadCount; // 방별 안읽은 수

    public static ChatRoomListDetailDto of(ChatRoom room, Users me, Long count,
                                           String major, String studentYear, String lastMessage) {

        Users opponent = room.getRequester().getUserId().equals(me.getUserId())
                ? room.getReceiver() : room.getRequester();

        return ChatRoomListDetailDto.builder()
                .roomId(room.getId())
                .opponentName(opponent.getName())
//                 .opponentProfileImg(opponent.getProfileImage())

                .opponentMajor(major)
                .opponentStudentYear(studentYear)
                .lastMessage(lastMessage)

                .unreadCount(count)
                .build();
    }
}