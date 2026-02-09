package CamNecT.CamNecT_Server.global.notification.controller;

import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.notification.dto.FCMSendRequest;
import CamNecT.CamNecT_Server.global.notification.dto.PushTestResponse;
import CamNecT.CamNecT_Server.global.notification.dto.RegisterPushTokenRequest;
import CamNecT.CamNecT_Server.global.notification.dto.RegisterPushTokenResponse;
import CamNecT.CamNecT_Server.global.notification.service.PushDeviceService;
import CamNecT.CamNecT_Server.global.notification.service.PushTestService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/push")
public class NotificationPushController {

    private final PushDeviceService pushDeviceService;
    private final PushTestService pushTestService;

    // 토큰 등록/갱신
    @PostMapping("/tokens")
    public ApiResponse<RegisterPushTokenResponse> registerToken(
            @UserId Long userId,
            @RequestBody @Valid RegisterPushTokenRequest req
    ) {
        var r = pushDeviceService.register(userId, req);
        return ApiResponse.success(new RegisterPushTokenResponse(r.pushDeviceId(), r.created()));
    }

    // 테스트 발송 (내 토큰들 대상으로)
    @PostMapping("/test")
    public ApiResponse<PushTestResponse> test(
            @UserId Long userId
    ) throws Exception {
        return ApiResponse.success(pushTestService.sendTest(userId));
    }

    @PostMapping("/debug/send")
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
}