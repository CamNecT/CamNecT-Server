package CamNecT.CamNecT_Server.domain.alumni.dto;

import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import lombok.Builder;

@Builder
public record UserProfileDto(
        Long userId,
        String bio, // 설명
        Boolean openToCoffeeChat, // 커피챗 허용 여부
        Boolean isFollowerVisible, // 팔로워 공개여부
        Boolean isEducationVisible, // 학력 공개여부
        Boolean isExperienceVisible, // 경력 공개여부
        Boolean isCertificateVisible, // 자격증 공개여부
        String profileImageUrl,  // presigned URL이 적용됨
        String studentNo, // 학번
        Integer yearLevel, // 학년
        Long institutionId, // 학교 id
        Long majorId // 전공 id
) {
    public static UserProfileDto from(UserProfile profile) {
        return UserProfileDto.builder()
                .userId(profile.getUserId())
                .bio(profile.getBio())
                .openToCoffeeChat(profile.getOpenToCoffeeChat())
                .isFollowerVisible(profile.getIsFollowerVisible())
                .isEducationVisible(profile.getIsEducationVisible())
                .isExperienceVisible(profile.getIsExperienceVisible())
                .isCertificateVisible(profile.getIsCertificateVisible())
                .profileImageUrl(profile.getProfileImageKey()) // 초기값은 원본 S3 key
                .studentNo(profile.getStudentNo())
                .yearLevel(profile.getYearLevel())
                .institutionId(profile.getInstitutionId())
                .majorId(profile.getMajorId())
                .build();
    }

    /**
     * 프로필 이미지 URL을 presigned URL로 교체한 새로운 객체 반환
     */
    public UserProfileDto withProfileImageUrl(String presignedUrl) {
        return UserProfileDto.builder()
                .userId(this.userId)
                .bio(this.bio)
                .openToCoffeeChat(this.openToCoffeeChat)
                .isFollowerVisible(this.isFollowerVisible)
                .isEducationVisible(this.isEducationVisible)
                .isExperienceVisible(this.isExperienceVisible)
                .isCertificateVisible(this.isCertificateVisible)
                .profileImageUrl(presignedUrl) // 새로운 URL 적용
                .studentNo(this.studentNo)
                .yearLevel(this.yearLevel)
                .institutionId(this.institutionId)
                .majorId(this.majorId)
                .build();
    }
}