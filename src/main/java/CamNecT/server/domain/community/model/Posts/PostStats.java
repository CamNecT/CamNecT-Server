package CamNecT.server.domain.community.model.Posts;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_stats_post", columnNames = "post_id"),
        indexes = {
                @Index(name = "idx_post_stats_hot", columnList = "hot_score,post_id"),
                @Index(name = "idx_post_stats_last", columnList = "last_activity_at,post_id"),
                @Index(name = "idx_post_stats_like", columnList = "like_count,post_id"),
                @Index(name = "idx_post_stats_bookmark", columnList = "bookmark_count,post_id"),
                @Index(name = "idx_post_stats_root", columnList = "root_comment_count,post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_stats_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Posts post;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "hot_score", nullable = false)
    private long hotScore;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount;

    @Column(name = "root_comment_count", nullable = false)
    private long rootCommentCount;

    @Builder.Default
    @Column(name = "like_rewarded_3", nullable = false)
    private boolean likeRewarded3 = false;


    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostStats init(Posts post) {
        LocalDateTime now = LocalDateTime.now();
        return PostStats.builder()
                .post(post)
                .likeCount(0)
                .commentCount(0)
                .viewCount(0)
                .hotScore(0)
                .bookmarkCount(0)
                .rootCommentCount(0)
                .lastActivityAt(now)
                .build();
    }

    public void touch() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void incView() {
        this.viewCount++;
        reHotS();
        touch();
    }

    public void incLike() { this.likeCount++; reHotS(); touch(); }
    public void decLike() { if (this.likeCount > 0) this.likeCount--; reHotS(); touch(); }

    public void incComment() { this.commentCount++; reHotS(); touch(); }
    public void decComment() { if (this.commentCount > 0) this.commentCount--; reHotS(); touch(); }

    private void reHotS() { //HotScore 계산 식(추후 수정 가능)
        this.hotScore = this.likeCount * 3L + this.commentCount * 5L + this.viewCount;
    }

    public void incBookmark() { this.bookmarkCount++; touch(); }
    public void decBookmark() { if (this.bookmarkCount > 0) this.bookmarkCount--; touch(); }

    public void incRootComment() { this.rootCommentCount++; touch(); }
    public void decRootComment() { if (this.rootCommentCount > 0) this.rootCommentCount--; touch(); }

    public boolean tryMarkLikeRewarded3() {
        if (this.likeRewarded3) return false;
        this.likeRewarded3 = true;
        return true;
    }
}