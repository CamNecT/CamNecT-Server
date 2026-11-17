package CamNecT.server.global.notification.debug;

import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.notification.dto.request.FCMSendRequest;
import CamNecT.server.global.notification.dto.NotificationPushPayload;
import CamNecT.server.global.notification.service.NotificationWsPublisher;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    })
    @io.swagger.v3.oas.annotations.Operation(summary = "알림 개발 환경 테스트: pushWsOnly")
    public ApiResponse<Void> pushWsOnly(@UserId Long userId) {
        var payload = new NotificationPushPayload(
                "DEBUG",
                "[WS 테스트]",
                "웹소켓 알림 테스트 메시지입니다.",
                null, null, null,
                "/chat/123"
        );
        wsPublisher.sendToUser(userId, payload);
        return ApiResponse.success(null);
    }

    @PostMapping("/event")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    })
    @io.swagger.v3.oas.annotations.Operation(summary = "알림 개발 환경 테스트: fireEvent")
    public ApiResponse<Void> fireEvent(@UserId Long userId) {
        debugService.fireTransactionalEvent(userId);
        return ApiResponse.success(null);
    }


    /// 여기서부터는 fcm테스트
    @PostMapping("/push/send")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청 본문 형식·필드 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @io.swagger.v3.oas.annotations.Operation(summary = "알림 개발 환경 테스트: send")
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", useReturnTypeSchema = true)
    })
    @io.swagger.v3.oas.annotations.Operation(summary = "알림 개발 환경 테스트: test")
    public ApiResponse<PushTestResponse> test(
            @UserId Long userId
    ) throws Exception {
        return ApiResponse.success(debugService.sendTest(userId));
    }
}
