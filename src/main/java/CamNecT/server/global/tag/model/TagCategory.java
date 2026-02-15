package CamNecT.server.global.tag.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tag_category",
        indexes = {
                @Index(
                        name = "idx_tag_category_active_sort",
                        columnList = "active, sort_order"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TagCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_category_id")
    private Long id;

    // field_major, job_skill, activity_spec ...
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    // 전공 및 학업, 직무 및 기술·스킬 ...
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}