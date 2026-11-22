package CamNecT.server.domain.report.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Only the cumulative count and continuing enforcement survive history expiry. */
@Entity
@Table(name = "expired_penalty_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpiredPenaltySummary {
    @Id
    private Long userId;
    private long penaltyCount;
    private boolean permanentlyBanned;
}
