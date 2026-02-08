package CamNecT.CamNecT_Server.domain.gifticon.service;

import CamNecT.CamNecT_Server.domain.gifticon.dto.response.BookmarkToggleResponse;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.GifticonHomeResponse;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.GifticonProductDetailResponse;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonBookmark;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonProduct;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonBookmarkRepository;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonProductRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GifticonService {

    private final GifticonProductRepository productRepository;
    private final GifticonBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final GifticonVendorClient vendorClient;

    @Value("${app.gifticon.vendor.enabled:false}")
    private boolean vendorEnabled;

    public enum Sort { POPULAR, PRICE_ASC, PRICE_DESC }

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

        var existing = bookmarkRepository.findByUser_UserIdAndProduct_Id(userId, productId).orElse(null);
        if (existing != null) {
            bookmarkRepository.delete(existing);
            return new BookmarkToggleResponse(false);
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GifticonErrorCode.USER_NOT_FOUND));

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

        // 업스트림이 일시적으로 비었을 때 "전체 비활성화" 방지
        if (vendorProducts == null || vendorProducts.isEmpty()) {
            return;
        }

        List<GifticonProduct> existing = productRepository.findAll();
        Map<String, GifticonProduct> existingMap = existing.stream()
                .collect(Collectors.toMap(GifticonProduct::getVendorProductCode, Function.identity(), (a, b) -> a));

        Set<String> seen = new HashSet<>();

        for (GifticonProduct.VendorSnapshot v : vendorProducts) {
            seen.add(v.vendorProductCode());

            GifticonProduct p = existingMap.get(v.vendorProductCode());
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

        // 이번 배치에 없던 상품 비활성화
        for (GifticonProduct p : existing) {
            if (!seen.contains(p.getVendorProductCode())) {
                p.deactivate(syncedAt);
            }
        }
    }
}