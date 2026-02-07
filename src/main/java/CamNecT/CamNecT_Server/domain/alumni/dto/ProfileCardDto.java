package CamNecT.CamNecT_Server.domain.alumni.dto;

import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import lombok.Builder;

@Builder
public record ProfileCardDto(
        String bio,
        Boolean openToCoffeeChat,
        String profileImageUrl,  // (현재 key일 수도 있음)
        String studentNo,
        Long majorId
) {
    public static ProfileCardDto from(UserProfile profile) {
        return ProfileCardDto.builder()
                .bio(profile.getBio())
                .openToCoffeeChat(profile.getOpenToCoffeeChat())
                .profileImageUrl(profile.getProfileImageKey())
                .studentNo(profile.getStudentNo())
                .majorId(profile.getMajorId())
                .build();
    }

    public ProfileCardDto withProfileImageUrl(String CdnUrl) {
        return ProfileCardDto.builder()
                .bio(this.bio)
                .openToCoffeeChat(this.openToCoffeeChat)
                .profileImageUrl(CdnUrl)
                .studentNo(this.studentNo)
                .majorId(this.majorId)
                .build();
    }
}
