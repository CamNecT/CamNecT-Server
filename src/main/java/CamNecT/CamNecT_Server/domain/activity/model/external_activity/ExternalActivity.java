package CamNecT.CamNecT_Server.domain.activity.model.external_activity;

import CamNecT.CamNecT_Server.domain.activity.dto.request.ActivityRequest;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityStatus;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "external_activities")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityCategory category;

    @Column(length = 100)
    private String organizer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(length = 50)
    private String region;

    @Column(length = 300)
    private String targetDescription;

    @Column(length = 500)
    private String thumbnailKey;

    @Column
    private LocalDate applyStartDate;

    @Column
    private LocalDate applyEndDate;

    private LocalDate resultAnnounceDate;

    @Column(length = 500)
    private String officialUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.OPEN;

    @Builder.Default
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String context;

    public void updateThumbnail(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public void update(ActivityRequest request) {
        this.title = request.title();
        this.category = request.category();
        this.context = request.content();
    }
}