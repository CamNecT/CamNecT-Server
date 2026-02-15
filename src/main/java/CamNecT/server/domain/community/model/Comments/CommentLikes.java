package CamNecT.server.domain.community.model.Comments;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_likes_comment_user",
                columnNames = {"comment_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_comment_likes_comment", columnList = "comment_id"),
                @Index(name = "idx_comment_likes_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommentLikes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @ToString.Exclude
    private Comments comment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CommentLikes of(Comments comment, Long userId) {
        return CommentLikes.builder().comment(comment).userId(userId).build();
    }
}