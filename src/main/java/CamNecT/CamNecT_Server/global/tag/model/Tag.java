package CamNecT.CamNecT_Server.global.tag.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tags",
        indexes = {
                @Index(name = "idx_tags_attr_active", columnList = "tag_attribute_id,active,tag_id"),
                @Index(name = "idx_tags_name", columnList = "name")
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

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    // ERD: category varchar(30) NULL (UI 섹션용: 학업/대외활동/진로/기타 등)
    @Column(name = "category", length = 30)
    private String category;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Tag create(String name, String category) {
        return Tag.builder()
                .name(name)
                .category(category)
                .active(true)
                .build();
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
}