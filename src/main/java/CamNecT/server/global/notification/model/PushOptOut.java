package CamNecT.server.global.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_opt_outs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushOptOut {
    @Id
    private Long userId;

    public PushOptOut(Long userId) {
        this.userId = userId;
    }
}
