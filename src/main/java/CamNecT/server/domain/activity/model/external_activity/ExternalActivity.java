package CamNecT.server.domain.activity.model.external_activity;

import CamNecT.server.domain.activity.dto.request.ActivityRequest;
import CamNecT.server.domain.activity.dto.request.AdminActivityRequest;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.enums.ActivityStatus;
import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

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

    @Column(name = "context_title", length = 200)
    private String contextTitle;

    @Lob
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    // 1. 태그 연결 관계 (활동 삭제 시 태그 매핑 데이터도 삭제)
    @OneToMany(mappedBy = "activity", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ExternalActivityTag> tags = new ArrayList<>();

    // 2. 북마크 연결 관계 (활동 삭제 시 유저들의 북마크 내역도 삭제)
    @OneToMany(mappedBy = "activity", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ExternalActivityBookmark> bookmarks = new ArrayList<>();

    // 3. 첨부파일 연결 관계 (활동 삭제 시 첨부파일 데이터도 삭제)
    @OneToMany(mappedBy = "activity", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ExternalActivityAttachment> attachments = new ArrayList<>();

    public void updateThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public void update(ActivityRequest request) {
        this.title = request.title();
        this.category = request.category();
        this.context = request.content();
    }

    public void updateAdmin(AdminActivityRequest request) {
        this.title = request.title();
        this.category = request.category();
        this.organizer = request.organizer();
        this.targetDescription = request.targetDescription();
        this.applyStartDate = request.applyStartDate();
        this.applyEndDate = request.applyEndDate();
        this.resultAnnounceDate = request.resultAnnounceDate();
        this.officialUrl = request.officialUrl();
        this.contextTitle = request.contextTitle();
        this.context = request.content();
    }

    public void close() {
        this.status = ActivityStatus.CLOSED;
    }
}