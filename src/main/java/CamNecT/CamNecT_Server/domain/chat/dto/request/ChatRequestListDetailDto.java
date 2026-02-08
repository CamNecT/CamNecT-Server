package CamNecT.CamNecT_Server.domain.chat.dto.request;

import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import lombok.Builder;


@Builder
public record ChatRequestListDetailDto(
        Long opponentId,
        String opponentName,
        String opponentMajor,
        String opponentStudentYear,
        String opponentProfileImg,

        Long requestId,
        String requestType,
        String requestContent
) {
    public static ChatRequestListDetailDto from(Users opponent,
                                                UserProfile opProfile,
                                                ChatRequest request,
                                                String majorName,
                                                String profileImgUrl) {

        return ChatRequestListDetailDto.builder()
                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())
                .opponentMajor(majorName)
                .opponentStudentYear(opProfile != null ? opProfile.getYearLevel().toString() : "")
                .opponentProfileImg(profileImgUrl)

                .requestType(request.getType().name())
                .requestContent(request.getContent())
                .build();
    }
}
