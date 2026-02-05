package CamNecT.CamNecT_Server.global.tag.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tags",
        indexes = {
                @Index(name = "idx_tags_name", columnList = "name"),
                @Index(name = "idx_tags_category_active", columnList = "tag_category_id, active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    // 경영학, 토스, 백엔드, SQLD ...
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_category_id", nullable = false)
    private TagCategory category;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
}