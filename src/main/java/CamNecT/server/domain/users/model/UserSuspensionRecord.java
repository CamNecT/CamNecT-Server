package CamNecT.server.domain.users.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_suspension_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSuspensionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suspension_record_id")
    private Long suspensionRecordId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "suspension_end_date")
    private LocalDateTime suspensionEndDate;

    @Builder.Default
    @Column(name = "is_permanently_banned", nullable = false)
    private boolean isPermanentlyBanned = false;

    @Column(name = "ban_reason")
    private String banReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void incrementReportCount() {
        this.reportCount++;
    }

    public void applySuspension(LocalDateTime endDate) {
        this.suspensionEndDate = endDate;
    }

    public void applyPermanentBan(String reason) {
        this.isPermanentlyBanned = true;
        this.banReason = reason;
    }

    public boolean isSuspended() {
        if (suspensionEndDate == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(suspensionEndDate);
    }

    public void clearSuspension() {
        this.suspensionEndDate = null;
    }
}
