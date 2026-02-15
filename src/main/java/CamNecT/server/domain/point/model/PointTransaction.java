package CamNecT.server.domain.point.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "PointTransaction",
        indexes = {
                @Index(name = "idx_point_tx_user", columnList = "user_id"),
                @Index(name = "idx_point_tx_event_key", columnList = "event_key")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_tx_event_key", columnNames = {"event_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_tx_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "request_id") // 커피챗 수락 시 사용
    private Long requestId;

    @Column(name = "point_change", nullable = false)
    private Integer pointChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType; // EARN, SPEND 등

    @Enumerated(EnumType.STRING) // 이 설정이 핵심입니다!
    @Column(name = "source_type", length = 50, nullable = false)
    private PointSource sourceType;

    @Column(name = "event_key", length = 64)
    private String eventKey;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

