package CamNecT.server.domain.alumni.dto;

import CamNecT.server.domain.users.model.UserProfile;
import lombok.Builder;

@Builder
public record ProfileCardDto(
        String bio,
        Boolean openToCoffeeChat,
        String profileImageUrl,  // (현재 key일 수도 있음)
        String studentNo,
        Long majorId
) {
    public static ProfileCardDto createCard(UserProfile profile, String profileImageUrl) {
        return ProfileCardDto.builder()
                .bio(profile.getBio())
                .openToCoffeeChat(profile.getOpenToCoffeeChat())
                .profileImageUrl(profileImageUrl)
                .studentNo(profile.getStudentNo())
                .majorId(profile.getMajorId())
                .build();
    }
}
