package CamNecT.CamNecT_Server.domain.community.model.Posts;

import CamNecT.CamNecT_Server.global.tag.model.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_tags_post_tag",
                columnNames = {"post_id", "tag_id"}
        ),
        indexes = {
                @Index(name = "idx_post_tags_tag_post", columnList = "tag_id,post_id"),
                @Index(name = "idx_post_tags_post", columnList = "post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostTags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostTags link(Posts post, Tag tag) {
        return PostTags.builder()
                .post(post)
                .tag(tag)
                .build();
    }
}
