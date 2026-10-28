package CamNecT.server.domain.verification.phone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verification_challenges")
@Getter
@NoArgsConstructor
public class PhoneChallenge {
    @Id @Column(name = "user_id") private Long userId;
    @Column(name = "challenge_id", nullable = false, length = 36) private String challengeId;
    @Column(name = "phone_hash", nullable = false, length = 64) private String phoneHash;
    @Column(name = "code_hash", nullable = false, length = 64) private String codeHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "attempts", nullable = false) private int attempts;
    @Enumerated(EnumType.STRING) @Column(name = "send_state", nullable = false, length = 16)
    private SendState sendState;
    @Column(name = "provider_request_id", length = 100) private String providerRequestId;

    public enum SendState { PENDING, SENT, FAILED }

    public static PhoneChallenge issue(Long userId, String challengeId, String phoneHash,
                                       String codeHash, LocalDateTime expiresAt) {
        PhoneChallenge c = new PhoneChallenge();
        c.userId = userId;
        c.challengeId = challengeId;
        c.phoneHash = phoneHash;
        c.codeHash = codeHash;
        c.expiresAt = expiresAt;
        c.sendState = SendState.PENDING;
        return c;
    }

    public void sent(String requestId) { sendState = SendState.SENT; providerRequestId = requestId; }
    public void failed() { sendState = SendState.FAILED; }
    public void recordFailure() { attempts++; }
}
