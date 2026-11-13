package CamNecT.server.domain.chat.dto.room;

import CamNecT.server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.tag.model.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomWithDetailDto {

    private Long roomId;
    private Long myId;
    private boolean isClosed;
    private boolean isOpponentExited;

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

    @With
    @Schema(description = "입장 직전 상대방이 보낸 미읽음 메시지 중 가장 오래된 ID. 없으면 null", nullable = true)
    private Long firstUnreadMessageId;
    private List<ChatMessageResponseDto> chatList;

    public static ChatRoomWithDetailDto from(ChatRoom room,
                                             Users me,
                                             Users opponent,
                                             UserProfile opProfile,
                                             String majorName,
                                             List<String> tagNames, List<ChatMessageResponseDto> chats, String title,
                                             String profileImgUrl, Long firstUnreadMessageId) {
        List<String> requestTagNames = (room.getTags() != null)
                ? room.getTags().stream().map(Tag::getName).toList()
                : List.of();

        boolean isMeRequester = room.getRequester().getUserId().equals(me.getUserId());
        boolean opponentExited = isMeRequester ? room.isReceiverExited() : room.isRequesterExited();

        return ChatRoomWithDetailDto.builder()
                .roomId(room.getId())
                .myId(me.getUserId())
                .isClosed(room.getStatus().equals(ChatRoom.RoomStatus.CLOSE))
                .isOpponentExited(opponentExited)

                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())

                .opponentMajor(majorName)
                .opponentStudentYear(opProfile != null && opProfile.getStudentNo() != null ? opProfile.getStudentNo() : "")
                .opponentProfileImg(profileImgUrl)
                .opponentTags(tagNames)

                .requestAt(room.getRequest().getCreatedAt() != null ? room.getRequest().getCreatedAt().toString() : "")
                .requestType(room.getRequest().getType().name())
                .requestTags(requestTagNames)
                .requestContent(room.getRequest().getContent())

                .recruitmentTitle(title)
                .activityId(room.getRequest().getActivityId())
                .recruitmentId(room.getRequest().getRecruitmentId())

                .firstUnreadMessageId(firstUnreadMessageId)
                .chatList(chats)
                .build();
    }
}
