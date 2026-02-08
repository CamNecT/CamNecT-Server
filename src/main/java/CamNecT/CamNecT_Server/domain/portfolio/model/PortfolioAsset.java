package CamNecT.CamNecT_Server.domain.portfolio.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PortfolioAsset")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id") // ERD 표기 준수
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioProject portfolioProject;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}