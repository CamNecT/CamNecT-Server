package CamNecT.server.domain.community.model.Posts;

import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_access",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_access_user_post",
                columnNames = {"user_id", "post_id"}
        ),
        indexes = {
                @Index(name = "idx_post_access_user", columnList = "user_id"),
                @Index(name = "idx_post_access_post", columnList = "post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_access_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Posts post;

    @Column(name = "paid_points", nullable = false)
    private int paidPoints;

    @CreationTimestamp
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    public static PostAccess of(Users user, Posts post, int paidPoints) {
        PostAccess pa = new PostAccess();
        pa.user = user;
        pa.post = post;
        pa.paidPoints = paidPoints;
        return pa;
    }
}
