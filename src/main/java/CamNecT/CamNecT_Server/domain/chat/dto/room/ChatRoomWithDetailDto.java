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

    private String requestAt;
    private String requestType;
    private List<String> requestTags;
    private String requestContent;

    private String recruitmentTitle;
    private Long activityId;
    private Long recruitmentId;

    private List<ChatMessageResponseDto> chatList;

    public static ChatRoomWithDetailDto from(ChatRoom room,
                                             Users me,
                                             Users opponent,
                                             UserProfile opProfile,
                                             String majorName,
                                             List<String> tagNames, List<ChatMessageResponseDto> chats, String title, String profileImgUrl) {
        List<String> requestTagNames = (room.getTags() != null)
                ? room.getTags().stream().map(Tag::getName).toList()
                : List.of();

        return ChatRoomWithDetailDto.builder()
                .roomId(room.getId())
                .myId(me.getUserId())

                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())

                .opponentMajor(majorName)
                .opponentStudentYear(opProfile != null && opProfile.getYearLevel() != null ? opProfile.getYearLevel().toString() : "")
                .opponentProfileImg(profileImgUrl)
                .opponentTags(tagNames)

                .requestAt(room.getRequest().getCreatedAt().toString())
                .requestType(room.getRequest().getType().name())
                .requestTags(requestTagNames)
                .requestContent(room.getRequest().getContent())

                .recruitmentTitle(title)
                .activityId(room.getRequest().getActivityId())
                .recruitmentId(room.getRequest().getRecruitmentId())

                .chatList(chats)
                .build();
    }
}