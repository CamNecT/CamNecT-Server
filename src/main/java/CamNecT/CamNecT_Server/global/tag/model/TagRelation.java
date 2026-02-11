package CamNecT.CamNecT_Server.global.tag.model;

import CamNecT.CamNecT_Server.global.tag.model.enums.TagRelationContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tag_relation",
        indexes = {
                @Index(name = "idx_tag_relation_from_score", columnList = "context,from_tag_id,score"),
                @Index(name = "idx_tag_relation_to", columnList = "context,to_tag_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TagRelation {

    @EmbeddedId
    private TagRelationId id;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "evidence_count", nullable = false)
    private Integer evidenceCount;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TagRelation of(TagRelationContext context, Long fromTagId, Long toTagId, double score, int evidenceCount) {
        return TagRelation.builder()
                .id(new TagRelationId(context, fromTagId, toTagId))
                .score(score)
                .evidenceCount(evidenceCount)
                .build();
    }

    public TagRelationContext context() { return id.getContext(); }
    public Long fromTagId() { return id.getFromTagId(); }
    public Long toTagId() { return id.getToTagId(); }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TagRelationId implements Serializable {

        @Enumerated(EnumType.STRING)
        @Column(name = "context", nullable = false, length = 32)
        private TagRelationContext context;

        @Column(name = "from_tag_id", nullable = false)
        private Long fromTagId;

        @Column(name = "to_tag_id", nullable = false)
        private Long toTagId;
    }
}

