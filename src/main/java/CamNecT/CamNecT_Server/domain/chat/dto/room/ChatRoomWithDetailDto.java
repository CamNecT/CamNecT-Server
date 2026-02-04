package CamNecT.CamNecT_Server.domain.chat.dto.room;

import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRoom;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomWithDetailDto {

    private Long roomId;
    private Long myId;

    private Long opponentId;
    private String opponentName;
    private String opponentMajor;
    private String opponentStudentYear;
    private String opponentProfileImg;
    private List<String> opponentTags;

    private String requestCategory;
    private List<String> requestTags;
    private String requestContent;

    private List<ChatMessageResponseDto> chatList;

    public static ChatRoomWithDetailDto from(ChatRoom room,
                                             Users me,
                                             Users opponent,
                                             UserProfile opProfile,
                                             String majorName,
                                             List<ChatMessageResponseDto> chats) {

        return ChatRoomWithDetailDto.builder()
                .roomId(room.getId())
                .myId(me.getUserId())

                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())

                .opponentMajor(majorName)
                .opponentStudentYear(opProfile != null ? opProfile.getYearLevel().toString() : "")
                .opponentProfileImg(opProfile != null ? opProfile.getProfileImageUrl() : "/images/default.png")

                .requestTags(room.getTags().stream()
                        .map(Tag::getName)
                        .toList())
                .requestContent(room.getRequest().getContent())

                .chatList(chats)
                .build();
    }
}