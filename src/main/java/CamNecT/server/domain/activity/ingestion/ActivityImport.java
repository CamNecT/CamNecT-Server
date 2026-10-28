package CamNecT.server.domain.activity.ingestion;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "activity_imports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityImport {
    @Id @Column(name = "source_id", length = 100) private String sourceId;
    // Deliberately not a cascading association: retain the tombstone after admin deletion.
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "source_revision", nullable = false) private long sourceRevision;
    @Column(name = "content_hash", length = 64, nullable = false) private String contentHash;
    @Column(name = "managed_hash", length = 64, nullable = false) private String managedHash;
    @Column(name = "thumbnail_hash", length = 64) private String thumbnailHash;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ActivityImport(String sourceId, Long activityId) {
        this.sourceId = sourceId;
        this.activityId = activityId;
    }
    public void applied(long revision, String contentHash, String managedHash, String thumbnailHash) {
        this.sourceRevision = revision;
        this.contentHash = contentHash;
        this.managedHash = managedHash;
        this.thumbnailHash = thumbnailHash;
        this.updatedAt = Instant.now();
    }
}
