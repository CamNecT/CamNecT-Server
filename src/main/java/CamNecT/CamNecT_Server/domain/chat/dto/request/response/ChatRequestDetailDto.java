package CamNecT.CamNecT_Server.domain.chat.dto.request.response;

import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatRequestDetailDto(
        Long myId,

        Long opponentId,
        String opponentName,
        String opponentMajor,
        String opponentStudentYear,
        String opponentProfileImg,
        List<String> opponentTags,

        Long requestId,
        String requestType,
        List<String> requestTags,
        String requestContent,
        String createdAt
) {

    public static ChatRequestDetailDto from(Users me,
                                            Users opponent,
                                            UserProfile opProfile,
                                            ChatRequest request,
                                            String majorName,
                                            List<String> opTagNames,
                                            String profileImgUrl) {

        return ChatRequestDetailDto.builder()
                .myId(me.getUserId())

                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())
                .opponentMajor(majorName)
                .opponentStudentYear(opProfile != null && opProfile.getYearLevel() != null ? opProfile.getYearLevel().toString() : "")
                .opponentProfileImg(profileImgUrl)
                .opponentTags(opTagNames)
                
                .requestId(request.getId())
                .requestType(request.getType().name())
                .requestTags(request.getRequestInterests().stream()
                        .map(Tag::getName)
                        .toList())
                .requestContent(request.getContent())
                .createdAt(request.getCreatedAt().toString())
                .build();
    }
}
