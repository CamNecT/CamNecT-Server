package CamNecT.CamNecT_Server.domain.verification.email.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "email_verification_tokens",
        indexes = {
                @Index(name = "idx_evt_user_used", columnList = "user_id, used_at")
        }
)
public class EmailVerificationToken {
    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //send 단계에서 user가 없으므로 email이 기준키
    @Column(name = "email", nullable = false)
    private String email;

    //verify 단계에서 user 객체 생성 후 주입
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    protected EmailVerificationToken(String email, Users user, String codeHash, LocalDateTime expiresAt) {
        this.email = email;
        this.user = user; // null 가능
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.usedAt = null;
        this.attemptCount = 0;
    }

    public static EmailVerificationToken issueForEmail(String email, String rawCode, long expirationMinutes) {
        return new EmailVerificationToken(
                email,
                null,
                EmailTokenUtil.sha256Hex(rawCode),
                LocalDateTime.now().plusMinutes(expirationMinutes)
        );
    }

    public void linkUser(Users user) {
        this.user = user;
    }

    public boolean isUsed() { return usedAt != null; }
    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isLocked() { return attemptCount >= MAX_ATTEMPTS; }

    public void markUsed() { this.usedAt = LocalDateTime.now(); }

    public void increaseAttempt() { this.attemptCount++; }

    public boolean matchesCode(String rawCode) {
        return EmailTokenUtil.sha256Hex(rawCode).equals(this.codeHash);
    }
}
