package CamNecT.CamNecT_Server.domain.users.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId // Users 엔티티의 PK를 그대로 UserProfile의 PK로 사용 (1:1 관계)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "open_to_coffeechat", nullable = false)
    @Builder.Default
    private Boolean openToCoffeeChat = false;

    @Column(name = "is_follower_visible", nullable = false)
    @Builder.Default
    private Boolean isFollowerVisible = true;

    @Column(name = "is_education_visible", nullable = false)
    @Builder.Default
    private Boolean isEducationVisible = true;

    @Column(name = "is_experience_visible", nullable = false)
    @Builder.Default
    private Boolean isExperienceVisible = true;

    @Column(name = "is_certificate_visible", nullable = false)
    @Builder.Default
    private Boolean isCertificateVisible = true;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "student_no", length = 20) //학번
    private String studentNo;

    @Column(name = "year_level") //학년
    private Integer yearLevel;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "major_id")
    private Long majorId;

    public void updateOnboardingProfile(String bio, String profileImageKey) {
        if (bio != null) {
            String b = bio.trim();
            this.bio = b.isBlank() ? null : b;
        }
        if (profileImageKey != null) {
            String u = profileImageKey.trim();
            this.profileImageUrl = u.isBlank() ? null : u;
        }
    }

    public void updateBio(String bio) {
        if (bio != null) {
            String b = bio.trim();
            this.bio = b.isBlank() ? null : b;
        }
    }

    public void applyVerifiedInfo(String studentName, String studentNo, Long institutionId, Long majorId) {
        this.user.updateName(studentName);
        this.studentNo = studentNo;
        this.institutionId = institutionId;
        this.majorId = majorId;
    }

    public void updatePrivacySettings(
            Boolean isFollowerVisible,
            Boolean isEducationVisible,
            Boolean isExperienceVisible,
            Boolean isCertificateVisible
    ) {
        if (isFollowerVisible != null) this.isFollowerVisible = isFollowerVisible;
        if (isEducationVisible != null) this.isEducationVisible = isEducationVisible;
        if (isExperienceVisible != null) this.isExperienceVisible = isExperienceVisible;
        if (isCertificateVisible != null) this.isCertificateVisible = isCertificateVisible;
    }
}