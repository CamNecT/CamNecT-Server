package CamNecT.CamNecT_Server.domain.users.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users") // 테이블명
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    //실질적 아이디라고 생각하시면 됩니다. -> 간단화해서 이메일을 아이디처럼..?
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    //일단 수집
    @Column(name = "phone_num", length = 20, unique = true)
    private String phoneNum;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Builder.Default //약관 동의1
    @Column(name = "terms_service_agreed", nullable = false)
    private boolean termsServiceAgreed = false;

    @Builder.Default //약관 동의2
    @Column(name = "terms_privacy_agreed", nullable = false)
    private boolean termsPrivacyAgreed = false;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default //
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status = UserStatus.EMAIL_PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Builder.Default
    @Column(name = "verification_complete_pending", nullable = false)
    private boolean verificationCompletePending = false;


    @CreationTimestamp // 생성 시 자동으로 시간 입력
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 수정 시 자동으로 시간 업데이트
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
    }


    //로그인시 User상태위한 메서드들
    public void markVerificationCompletePending() { this.verificationCompletePending = true; }
    public void clearVerificationCompletePending() { this.verificationCompletePending = false; }
}