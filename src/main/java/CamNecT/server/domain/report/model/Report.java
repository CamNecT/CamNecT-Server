package CamNecT.server.domain.report.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private Long reporterId; // 신고자 ID

    @Column(nullable = false)
    private Long reportedUserId; // 신고당한 유저 ID

    private Long reportedPostId; // 신고 글 ID (NULL 허용)

    @Enumerated(EnumType.STRING)
    private TargetType postType; // 신고 글 타입 (COMMUNITY, ACTIVITY 등)

    @Column(nullable = false)
    private String reportCategory; // 신고 유형

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.RECEIVED; // 기본값 RECEIVED

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 편의를 위한 빌더 패턴 또는 생성자
    public Report(Long reporterId, Long reportedUserId, Long reportedPostId,
                  TargetType postType, String reportCategory, String title, String context) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reportedPostId = reportedPostId;
        this.postType = postType;
        this.reportCategory = reportCategory;
        this.title = title;
        this.context = context;
        this.status = ReportStatus.RECEIVED;
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}