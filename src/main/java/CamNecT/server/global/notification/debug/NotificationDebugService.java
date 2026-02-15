package CamNecT.server.global.notification.debug;

import CamNecT.server.global.notification.util.FCMSender;
import CamNecT.server.global.notification.service.PushDeviceService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Profile({"local", "dev"})
@Service
@RequiredArgsConstructor
public class NotificationDebugService {

    private final ApplicationEventPublisher publisher;
    private final PushDeviceService pushDeviceService;
    private final FCMSender fcmSender;

    @Transactional
    public void fireTransactionalEvent(Long userId) {
        publisher.publishEvent(new DebugNotifiableEvent(
                userId,
                null,
                999L,
                "디버그 이벤트로 발생한 메시지입니다."
        ));
        // 트랜잭션 커밋 시점에 BEFORE/AFTER 리스너가 순서대로 실행됨
    }

    @Transactional
    public PushTestResponse sendTest(Long userId) throws FirebaseMessagingException {
        var tokens = pushDeviceService.findEnabledTokens(userId);

        var result = fcmSender.sendToTokens(
                tokens,
                "Camnect 테스트 푸시",
                "FCM 연결 확인용 테스트 메시지입니다.",
                Map.of("type", "TEST")
        );

        // 무효 토큰 정리
        pushDeviceService.disableTokens(result.invalidTokens());

        return new PushTestResponse(
                result.requested(),
                result.success(),
                result.failure(),
                result.invalidTokens().size(),
                result.invalidTokens()
        );
    }
}