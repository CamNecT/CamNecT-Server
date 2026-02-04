package CamNecT.CamNecT_Server.domain.community.model.Posts;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_bookmarks_post_user",
                columnNames = {"post_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_post_bookmarks_user", columnList = "user_id,post_id"),
                @Index(name = "idx_post_bookmarks_post", columnList = "post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostBookmarks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostBookmarks create(Posts post, Users user) {
        return PostBookmarks.builder()
                .post(post)
                .user(user)
                .build();
    }
}