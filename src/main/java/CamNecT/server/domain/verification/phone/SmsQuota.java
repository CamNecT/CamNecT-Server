package CamNecT.server.domain.verification.phone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_sms_quotas")
@Getter
@NoArgsConstructor
public class SmsQuota {
    @Id @Column(name = "quota_key", length = 100) private String key;
    @Column(name = "send_count", nullable = false) private int count;
    @Column(name = "last_sent_at") private LocalDateTime lastSentAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;

    public SmsQuota(String key, LocalDateTime expiresAt) {
        this.key = key;
        this.expiresAt = expiresAt;
    }

    public void consume(LocalDateTime now) { count++; lastSentAt = now; }

    public void resetIfExpired(LocalDateTime now, LocalDateTime end) {
        if (!expiresAt.isAfter(now)) { count = 0; expiresAt = end; }
    }
}
