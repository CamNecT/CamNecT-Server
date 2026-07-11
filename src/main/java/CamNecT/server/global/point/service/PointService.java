package CamNecT.server.global.point.service;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.point.model.*;
import CamNecT.server.global.point.repository.PointTransactionRepository;
import CamNecT.server.global.point.repository.PointWalletRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.server.global.notification.event.SimpleNotifiableEvent;
import CamNecT.server.global.notification.model.NotificationType;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointWalletRepository walletRepository;
    private final PointTransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Transactional
    public void spendPoint(Long userId, int amount, PointEvent event) {
        changePoint(userId, amount, TransactionType.SPEND, event);
    }

    @Transactional
    public void earnPoint(Long userId, int amount, PointEvent event) {
        changePoint(userId, amount, TransactionType.EARN, event);
    }

    @Transactional
    public void changePoint(Long userId, int amount, TransactionType type, PointEvent event) {
        if (event == null || event.source() == null) {
            throw new CustomException(UserErrorCode.POINT_EVENT_REQUIRED);
        }


        try {
            if (event.eventKey() != null && transactionRepository.existsByEventKey(event.eventKey())) {
                return;
            }

            PointWallet wallet = getOrCreateWallet(userId);

            if (type == TransactionType.SPEND && wallet.getBalance() < amount) {
                throw new CustomException(UserErrorCode.INSUFFICIENT_POINT);
            }

            int signedAmount = type == TransactionType.SPEND ? -amount : amount;
            wallet.updateBalance(signedAmount);

            PointTransaction tx = PointTransaction.builder()
                    .userId(userId)
                    .postId(event.postId())
                    .requestId(event.requestId())
                    .pointChange(signedAmount)
                    .transactionType(type)
                    .sourceType(event.source())
                    .eventKey(event.eventKey())
                    .balanceAfter(wallet.getBalance())
                    .build();

            transactionRepository.save(tx);
            walletRepository.flush();

            /// 알림 이벤트 발행 (저장 성공 시점)
            NotificationType nType = (type == TransactionType.EARN) ? NotificationType.POINT_EARNED : NotificationType.POINT_SPENT;

            String msg = (type == TransactionType.EARN) ? amount + "P가 지급되었습니다." : amount + "P를 사용했습니다.";

            // postId는 event에 있을 수도 있으니 그대로 연결
            eventPublisher.publishEvent(SimpleNotifiableEvent.ofAllowSelf(
                    userId,      // receiver
                    null,        // actor (시스템)
                    nType,
                    msg,
                    event.postId(),
                    null
            ));

        } catch (OptimisticLockException e) {
            throw new CustomException(ErrorCode.CONFLICT);
        } catch (DataIntegrityViolationException e) {
            if (event.eventKey() != null) return; // eventKey 유니크 충돌이면 멱등 처리
            throw e;
        }
    }

    private PointWallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return walletRepository.save(
                                PointWallet.builder()
                                        .userId(userId)
                                        .balance(0)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new CustomException(UserErrorCode.WALLET_CREATE_FAILED, e));
                    }
                });
    }

    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(PointWallet::getBalance)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public String getPhoneNum(Long userId) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return user.getPhoneNum();
    }
}