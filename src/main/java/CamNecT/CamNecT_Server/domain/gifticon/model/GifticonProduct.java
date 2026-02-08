package CamNecT.CamNecT_Server.domain.gifticon.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "gifticon_products",
        uniqueConstraints = @UniqueConstraint(name = "uk_gifticon_vendor_code", columnNames = "vendor_product_code"),
        indexes = {
                @Index(name = "idx_gifticon_product_active", columnList = "is_active"),
                @Index(name = "idx_gifticon_product_price", columnList = "price_points")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GifticonProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "vendor_product_code", nullable = false, length = 100)
    private String vendorProductCode;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "price_points", nullable = false)
    private Integer pricePoints;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "detail_image_url", length = 500)
    private String detailImageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_score")
    @Builder.Default
    private Integer sortScore = 0;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateFromVendor(VendorSnapshot v, LocalDateTime syncedAt) {
        this.brandName = v.brandName();
        this.productName = v.productName();
        this.pricePoints = v.pricePoints();
        this.imageUrl = v.imageUrl();
        this.detailImageUrl = v.detailImageUrl();
        this.isActive = true;
        this.sortScore = (v.sortScore() == null ? 0 : v.sortScore());
        this.lastSyncedAt = syncedAt;
    }

    public void deactivate(LocalDateTime syncedAt) {
        this.isActive = false;
        this.lastSyncedAt = syncedAt;
    }

    public record VendorSnapshot(
            String vendorProductCode,
            String brandName,
            String productName,
            Integer pricePoints,
            String imageUrl,
            String detailImageUrl,
            Integer sortScore
    ) {}
}