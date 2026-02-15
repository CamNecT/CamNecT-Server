package CamNecT.server.domain.activity.model.recruitment;

import CamNecT.server.domain.activity.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeamApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "recruit_id", nullable = false)
    private Long recruitId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.REQUESTED;

    @Column(name = "content", nullable = false, length = 100)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 상태 변경 메서드 (비즈니스 로직용)
    public void updateStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
    }
}