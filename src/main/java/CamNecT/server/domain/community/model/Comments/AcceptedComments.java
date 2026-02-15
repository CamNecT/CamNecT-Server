package CamNecT.server.domain.community.model.Comments;

import CamNecT.server.domain.community.model.Posts.Posts;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "accepted_comments",
        uniqueConstraints = @UniqueConstraint(name = "uk_accepted_comments_post", columnNames = "post_id"),
        indexes = {
                @Index(name = "idx_accepted_comments_post", columnList = "post_id"),
                @Index(name = "idx_accepted_comments_comment", columnList = "comment_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AcceptedComments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accepted_comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @ToString.Exclude
    private Comments comment;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 채택한 사람(질문 작성자)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AcceptedComments of(Posts post, Comments comment, Long userId) {
        return AcceptedComments.builder().post(post).comment(comment).userId(userId).build();
    }
}
