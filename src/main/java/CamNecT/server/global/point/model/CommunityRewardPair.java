package CamNecT.server.global.point.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_reward_pairs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityRewardPair {
    @Embeddable
    public record Key(String sourceType, Long lowUserId, Long highUserId) implements Serializable {}

    @EmbeddedId
    private Key id;
    private LocalDateTime rewardedAt;
}
