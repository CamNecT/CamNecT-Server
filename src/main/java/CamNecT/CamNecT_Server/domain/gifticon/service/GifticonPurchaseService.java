package CamNecT.CamNecT_Server.domain.gifticon.service;

import CamNecT.CamNecT_Server.domain.gifticon.dto.request.ConfirmGifticonPurchaseRequest;
import CamNecT.CamNecT_Server.domain.gifticon.dto.response.GifticonPurchaseConfirmResponse;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonProduct;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonPurchase;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonProductRepository;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonPurchaseRepository;
import CamNecT.CamNecT_Server.domain.point.model.PointEvent;
import CamNecT.CamNecT_Server.domain.point.model.PointWallet;
import CamNecT.CamNecT_Server.domain.point.repository.PointTransactionRepository;
import CamNecT.CamNecT_Server.domain.point.repository.PointWalletRepository;
import CamNecT.CamNecT_Server.domain.point.service.PointService;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.GifticonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GifticonPurchaseService {

    private final GifticonProductRepository productRepository;
    private final GifticonPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional
    public GifticonPurchaseConfirmResponse confirm(Long userId, ConfirmGifticonPurchaseRequest req) {

        // 1) 멱등 처리: (userId, clientRequestId)로 먼저 조회
        if (req.clientRequestId() != null && !req.clientRequestId().isBlank()) {
            GifticonPurchase exists = purchaseRepository
                    .findByUser_UserIdAndClientRequestId(userId, req.clientRequestId())
                    .orElse(null);
            if (exists != null) {
                return new GifticonPurchaseConfirmResponse(exists.getId(), exists.getRequestedAt());
            }
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GifticonErrorCode.USER_NOT_FOUND));

        GifticonProduct product = productRepository.findById(req.productId())
                .orElseThrow(() -> new CustomException(GifticonErrorCode.PRODUCT_NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new CustomException(GifticonErrorCode.PRODUCT_INACTIVE);
        }

        int qty = req.quantity();
        if (qty <= 0 || qty > 99) {
            throw new CustomException(GifticonErrorCode.INVALID_QUANTITY);
        }

        long expected = (long) product.getPricePoints() * qty;
        if (req.spendPoints().longValue() != expected) {
            throw new CustomException(GifticonErrorCode.INVALID_SPEND_POINTS);
        }

        // 2) 구매요청 적재(스냅샷)
        GifticonPurchase purchase = GifticonPurchase.builder()
                .user(user)
                .product(product)
                .clientRequestId(req.clientRequestId())
                .quantity(qty)
                .unitPricePoints(product.getPricePoints())
                .totalPricePoints((int) expected)

                .buyerName(user.getName())
                .buyerPhone(user.getPhoneNum())  // Users에 없으면 null 처리/필드명 수정
                .buyerEmail(user.getEmail())  // Users에 없으면 null 처리/필드명 수정

                .recipientName(blankToNull(req.recipientName()))
                .recipientPhone(blankToNull(req.recipientPhone()))
                .giftMessage(blankToNull(req.giftMessage()))
                .requestedAt(LocalDateTime.now())
                .build();

        try {
            purchaseRepository.saveAndFlush(purchase);
        } catch (DataIntegrityViolationException e) {
            // 동시 호출로 unique 충돌이면 “기존거 반환”으로 처리
            if (req.clientRequestId() != null && !req.clientRequestId().isBlank()) {
                GifticonPurchase exists = purchaseRepository
                        .findByUser_UserIdAndClientRequestId(userId, req.clientRequestId())
                        .orElseThrow(() -> new CustomException(GifticonErrorCode.DUPLICATE_REQUEST));
                return new GifticonPurchaseConfirmResponse(exists.getId(), exists.getRequestedAt());
            }
            throw e;
        }

        // 3) 포인트 차감 (PointService 사용)
        // 멱등키(eventKey)는 clientRequestId 기반으로 잡습니다.
        PointEvent event = PointEvent.gifticonPurchase(userId, purchase.getId(), req.clientRequestId());
        pointService.spendPoint(userId, (int) expected, event);

        return new GifticonPurchaseConfirmResponse(purchase.getId(), purchase.getRequestedAt());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}