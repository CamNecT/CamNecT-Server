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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
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

    protected EmailVerificationToken(Users user, String codeHash, LocalDateTime expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.usedAt = null;
        this.attemptCount = 0;
    }

    public static EmailVerificationToken issue(Users user, String rawCode, long expirationMinutes) {
        return new EmailVerificationToken(
                user,
                EmailTokenUtil.sha256Hex(rawCode),
                LocalDateTime.now().plusMinutes(expirationMinutes)
        );
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
