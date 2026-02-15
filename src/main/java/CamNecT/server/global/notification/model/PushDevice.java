package CamNecT.server.global.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "push_devices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_push_devices_user_device",
                columnNames = {"user_id", "device_id"}
        ),
        indexes = {
                @Index(name = "idx_push_devices_user_enabled", columnList = "user_id, enabled"),
                @Index(name = "idx_push_devices_token", columnList = "fcm_token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_device_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    public void updateToken(Platform platform, String fcmToken) {
        this.platform = platform;
        this.fcmToken = fcmToken;
        this.enabled = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.lastSeenAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.lastSeenAt == null) this.lastSeenAt = LocalDateTime.now();
    }
}
