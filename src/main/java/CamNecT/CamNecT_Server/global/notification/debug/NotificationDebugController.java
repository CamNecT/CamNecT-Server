package CamNecT.CamNecT_Server.global.notification.debug;

import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.notification.dto.FCMSendRequest;
import CamNecT.CamNecT_Server.global.notification.dto.NotificationPushPayload;
import CamNecT.CamNecT_Server.global.notification.service.NotificationWsPublisher;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debug/notification/")
public class NotificationDebugController {

    private final NotificationWsPublisher wsPublisher;
    private final NotificationDebugService debugService;

    @PostMapping("/ws")
    public ApiResponse<Void> pushWsOnly(@UserId Long userId) {
        var payload = new NotificationPushPayload(
                "DEBUG",
                "[WS 테스트]",
                "웹소켓 알림 테스트 메시지입니다.",
                null, null, null,
                "/chat/room/123"
        );
        wsPublisher.sendToUser(userId, payload);
        return ApiResponse.success(null);
    }

    @PostMapping("/event")
    public ApiResponse<Void> fireEvent(@UserId Long userId) {
        debugService.fireTransactionalEvent(userId);
        return ApiResponse.success(null);
    }


    /// 여기서부터는 fcm테스트
    @PostMapping("/push/send")
    public ApiResponse<Map<String, Object>> send(@RequestBody @Valid FCMSendRequest req) throws Exception {

        Message msg = Message.builder()
                .setToken(req.token())
                .setNotification(Notification.builder()
                        .setTitle(req.title())
                        .setBody(req.body())
                        .build())
                .putAllData(req.data() == null ? Map.of() : req.data())
                .build();

        String messageId = FirebaseMessaging.getInstance().send(msg);

        return ApiResponse.success(Map.of(
                "messageId", messageId,
                "requestedToken", req.token()
        ));
    }

    // 테스트 발송 (내 토큰들 대상으로)
    @PostMapping("/test/send")
    public ApiResponse<PushTestResponse> test(
            @UserId Long userId
    ) throws Exception {
        return ApiResponse.success(debugService.sendTest(userId));
    }
}