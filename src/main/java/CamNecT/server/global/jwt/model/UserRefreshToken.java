package CamNecT.server.global.jwt.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserRefreshToken {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void rotate(String newHash, LocalDateTime newExpiresAt) {
        this.refreshTokenHash = newHash;
        this.expiresAt = newExpiresAt;
        this.updatedAt = LocalDateTime.now();
    }
}