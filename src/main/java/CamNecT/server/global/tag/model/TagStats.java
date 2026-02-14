package CamNecT.server.global.tag.model;

import CamNecT.server.global.tag.model.enums.TagRelationContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tag_stats",
        indexes = {
                @Index(name = "idx_tag_stats_context_count", columnList = "context,doc_count")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TagStats {

    @EmbeddedId
    private TagStatsId id;

    @Column(name = "doc_count", nullable = false)
    private Integer docCount;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TagStats of(TagRelationContext context, Long tagId, int docCount) {
        return TagStats.builder()
                .id(new TagStatsId(context, tagId))
                .docCount(docCount)
                .build();
    }

    public TagRelationContext context() { return id.getContext(); }
    public Long tagId() { return id.getTagId(); }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TagStatsId implements Serializable {

        @Enumerated(EnumType.STRING)
        @Column(name = "context", nullable = false, length = 32)
        private TagRelationContext context;

        @Column(name = "tag_id", nullable = false)
        private Long tagId;
    }
}

