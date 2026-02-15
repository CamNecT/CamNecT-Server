package CamNecT.server.domain.chat.dto.request.response;

import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.tag.model.Tag;
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
        String createdAt,

        String recruitmentTitle,
        Long activityId,
        Long recruitmentId
) {

    public static ChatRequestDetailDto from(Users me,
                                            Users opponent,
                                            ChatRequest request,
                                            String majorName,
                                            String StudentNo,
                                            List<String> opTagNames,
                                            String profileImgUrl, String title) {

        return ChatRequestDetailDto.builder()
                .myId(me.getUserId())

                .opponentId(opponent.getUserId())
                .opponentName(opponent.getName())
                .opponentMajor(majorName)
                .opponentStudentYear(StudentNo)
                .opponentProfileImg(profileImgUrl)
                .opponentTags(opTagNames)
                
                .requestId(request.getId())
                .requestType(request.getType().name())
                .requestTags(request.getRequestInterests().stream()
                        .map(Tag::getName)
                        .toList())
                .requestContent(request.getContent())
                .createdAt(request.getCreatedAt().toString())

                .recruitmentTitle(title)
                .activityId(request.getActivityId())
                .recruitmentId(request.getRecruitmentId())
                .build();
    }
}
