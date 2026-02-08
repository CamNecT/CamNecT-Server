package CamNecT.CamNecT_Server.domain.activity.model.external_activity;

import CamNecT.CamNecT_Server.domain.activity.dto.request.ActivityRequest;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_activities")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ActivityCategory category;

    @Column(name = "organizer", length = 100)
    private String organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "target_description", length = 300)
    private String targetDescription;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "apply_start_date")
    private LocalDate applyStartDate;

    @Column(name = "apply_end_date")
    private LocalDate applyEndDate;

    @Column(name = "result_announce_date")
    private LocalDate resultAnnounceDate;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Lob
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void update(ActivityRequest request) {
        this.title = request.title();
        this.category = request.category();
        this.context = request.content();
    }
}