package CamNecT.server.domain.portfolio.model;

import CamNecT.server.global.common.util.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PortfolioProject")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Setter
public class PortfolioProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    @Builder.Default
    private String thumbnailUrl = "기본이미지";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private boolean isFavorite = false;

    @Column(name = "assigned_role", nullable = false)
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> assignedRole = new ArrayList<>();

    @Column(name = "tech_stack", nullable = false)
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> techStack = new ArrayList<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt; // 생성일 (DATE 타입)

    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt; // 수정일 (DATE 타입)

    @OneToMany(mappedBy = "portfolioProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PortfolioAsset> assets = new ArrayList<>();

    public void updateThumbnail(String url) {
        this.thumbnailUrl = url;
    }

    // PortfolioProject.java 내 수정
    public void updateInfo(String title, String description, String review,
                           LocalDate startDate, LocalDate endDate,
                           String projectRole, List<String> techStack) {
        this.title = title;
        this.description = description;
        this.review = review;
        this.startDate = startDate;
        this.endDate = endDate;
        // String으로 받은 역할을 List로 변환하여 저장
        this.assignedRole = (projectRole != null) ? List.of(projectRole) : new ArrayList<>();
        this.techStack = (techStack != null) ? techStack : new ArrayList<>();
        this.updatedAt = LocalDate.now();
    }
}