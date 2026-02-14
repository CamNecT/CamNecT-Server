package CamNecT.server.domain.gifticon.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

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

        Optional.ofNullable(v.brandName())
                .filter(StringUtils::hasText)
                .ifPresent(s -> this.brandName = s);

        Optional.ofNullable(v.productName())
                .filter(StringUtils::hasText)
                .ifPresent(s -> this.productName = s);

        Optional.ofNullable(v.pricePoints())
                .ifPresent(pp -> this.pricePoints = pp);

        Optional.ofNullable(v.imageUrl())
                .filter(StringUtils::hasText)
                .ifPresent(s -> this.imageUrl = s);

        Optional.ofNullable(v.sortScore())
                .ifPresent(s -> this.sortScore = s);

        this.isActive = true;
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
            Integer sortScore
    ) {}
}