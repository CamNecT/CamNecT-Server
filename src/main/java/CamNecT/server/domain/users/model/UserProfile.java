package CamNecT.server.domain.users.model;

import CamNecT.server.domain.profile.components.majors.model.Majors;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

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
    private Boolean openToCoffeeChat = true;

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

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Column(name = "student_no", length = 20) //학번
    private String studentNo;

    @Column(name = "year_level") //학년
    private Integer yearLevel;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "major_id")
    private Long majorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", referencedColumnName = "major_id",
            insertable = false, updatable = false)
    private Majors major;

    public void updateOnboardingProfile(String bio, String profileImageKey) {
        updateBio(bio);
        updateProfileImageKey(profileImageKey);
    }

    public void updateBio(String bio) {
        if (bio != null) {
            String b = bio.trim();
            this.bio = b.isBlank() ? null : b;
        }
    }

    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = StringUtils.hasText(profileImageKey) ? profileImageKey.trim() : null;
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