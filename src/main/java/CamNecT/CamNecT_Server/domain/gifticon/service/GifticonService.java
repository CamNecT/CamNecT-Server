package CamNecT.CamNecT_Server.domain.gifticon.service;

import CamNecT.CamNecT_Server.domain.gifticon.dto.response.BookmarkToggleResponse;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.GifticonHomeResponse;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.GifticonProductDetailResponse;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonBookmark;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonProduct;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonBookmarkRepository;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonProductRepository;
import CamNecT.CamNecT_Server.domain.point.repository.PointWalletRepository;
import CamNecT.CamNecT_Server.domain.point.service.PointService;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.GifticonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GifticonService {

    private final GifticonProductRepository productRepository;
    private final GifticonBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    private final PointWalletRepository pointWalletRepository;

    private final GifticonVendorClient vendorClient;

    @Value("${app.gifticon.vendor.enabled:false}")
    private boolean vendorEnabled;

    public enum Sort {
        POPULAR, PRICE_ASC, PRICE_DESC
    }

    public GifticonHomeResponse home(Long userId, Sort sort) {
        long myPoint = pointService.getBalance(userId);

        List<GifticonProduct> products = switch (sort) {
            case PRICE_ASC -> productRepository.findAllByIsActiveTrueOrderByPricePointsAscIdDesc();
            case PRICE_DESC -> productRepository.findAllByIsActiveTrueOrderByPricePointsDescIdDesc();
            default -> productRepository.findAllByIsActiveTrueOrderBySortScoreDescIdDesc();
        };

        Set<Long> bookmarkedIds = bookmarkRepository.findAllByUser_UserId(userId).stream()
                .map(b -> b.getProduct().getId())
                .collect(Collectors.toSet());

        LocalDateTime lastSyncedAt = products.stream()
                .map(GifticonProduct::getLastSyncedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<GifticonHomeResponse.ProductView> views = products.stream()
                .map(p -> new GifticonHomeResponse.ProductView(
                        p.getId(),
                        p.getBrandName(),
                        p.getProductName(),
                        p.getPricePoints(),
                        p.getImageUrl(),
                        bookmarkedIds.contains(p.getId()),
                        Boolean.TRUE.equals(p.getIsActive())
                ))
                .toList();

        return new GifticonHomeResponse(myPoint, views, lastSyncedAt);
    }

    public GifticonProductDetailResponse productDetail(Long userId, Long productId) {
        GifticonProduct p = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(GifticonErrorCode.PRODUCT_NOT_FOUND));

        boolean bookmarked = bookmarkRepository.existsByUser_UserIdAndProduct_Id(userId, productId);

        return new GifticonProductDetailResponse(
                p.getId(),
                p.getBrandName(),
                p.getProductName(),
                p.getPricePoints(),
                p.getImageUrl(),
                p.getDetailImageUrl(),
                bookmarked,
                Boolean.TRUE.equals(p.getIsActive())
        );
    }

    @Transactional
    public BookmarkToggleResponse toggleBookmark(Long userId, Long productId) {
        GifticonProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(GifticonErrorCode.PRODUCT_NOT_FOUND));

        GifticonBookmark existing = bookmarkRepository.findByUser_UserIdAndProduct_Id(userId, productId).orElse(null);
        if (existing != null) {
            bookmarkRepository.delete(existing);
            return new BookmarkToggleResponse(false);
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GifticonErrorCode.PRODUCT_NOT_FOUND)); // USER 에러코드로 바꿔도 됨

        bookmarkRepository.save(GifticonBookmark.builder()
                .user(user)
                .product(product)
                .build());

        return new BookmarkToggleResponse(true);
    }

    /**
     * (스케줄러에서 호출) 업체 상품목록을 받아 DB 캐시 갱신
     */
    @Transactional
    public void syncCatalogFromVendor() {
        if (!vendorEnabled) return;

        LocalDateTime syncedAt = LocalDateTime.now();
        List<GifticonProduct.VendorSnapshot> vendorProducts;
        try {
            vendorProducts = vendorClient.fetchProducts();
        } catch (Exception e) {
            throw new CustomException(GifticonErrorCode.VENDOR_SYNC_FAILED);
        }

        Map<String, GifticonProduct.VendorSnapshot> vendorMap = vendorProducts.stream()
                .collect(Collectors.toMap(GifticonProduct.VendorSnapshot::vendorProductCode, v -> v, (a, b) -> a));

        List<GifticonProduct> existing = productRepository.findAll();
        Set<String> seen = new HashSet<>();

        for (GifticonProduct.VendorSnapshot v : vendorProducts) {
            seen.add(v.vendorProductCode());

            GifticonProduct p = productRepository.findByVendorProductCode(v.vendorProductCode()).orElse(null);
            if (p == null) {
                productRepository.save(GifticonProduct.builder()
                        .vendorProductCode(v.vendorProductCode())
                        .brandName(v.brandName())
                        .productName(v.productName())
                        .pricePoints(v.pricePoints())
                        .imageUrl(v.imageUrl())
                        .detailImageUrl(v.detailImageUrl())
                        .isActive(true)
                        .sortScore(v.sortScore() == null ? 0 : v.sortScore())
                        .lastSyncedAt(syncedAt)
                        .build());
            } else {
                p.updateFromVendor(v, syncedAt);
            }
        }

        // 이번 배치에 없던 상품은 비활성화
        for (GifticonProduct p : existing) {
            if (!seen.contains(p.getVendorProductCode())) {
                p.deactivate(syncedAt);
            }
        }
    }

    /**
     * ⚠️ 포인트 연동부
     * - 여기 메서드는 프로젝트 point 도메인 구현에 따라 메서드명 조정이 필요할 수 있습니다.
     */
    private long loadMyPoint(Long userId) {
        // 케이스 A) PointWallet의 PK가 userId인 구조면: pointWalletRepository.findById(userId)
        // 케이스 B) user 엔티티 연관이면: pointWalletRepository.findByUser_UserId(userId)
        // 아래는 “둘 중 하나로 맞춰” 쓰면 됩니다.

        return pointWalletRepository.findById(userId)
                .map(w -> {
                    // TODO: PointWallet의 실제 필드/게터명에 맞추세요 (balance / point / amount 등)
                    try {
                        return (Number) w.getClass().getMethod("getBalance").invoke(w);
                    } catch (Exception ignore) {
                        try {
                            return (Number) w.getClass().getMethod("getPoint").invoke(w);
                        } catch (Exception e) {
                            // 최후: 0
                            return 0L;
                        }
                    }
                })
                .map(Number::longValue)
                .orElse(0L);
    }
}