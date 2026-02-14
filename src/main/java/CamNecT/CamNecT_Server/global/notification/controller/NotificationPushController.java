package CamNecT.CamNecT_Server.global.notification.controller;

import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.notification.dto.RegisterPushTokenRequest;
import CamNecT.CamNecT_Server.global.notification.dto.RegisterPushTokenResponse;
import CamNecT.CamNecT_Server.global.notification.service.PushDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/push")
public class NotificationPushController {

    private final PushDeviceService pushDeviceService;

    // 토큰 등록/갱신
    @PostMapping("/tokens")
    public ApiResponse<RegisterPushTokenResponse> registerToken(
            @UserId Long userId,
            @RequestBody @Valid RegisterPushTokenRequest req
    ) {
        var r = pushDeviceService.register(userId, req);
        return ApiResponse.success(new RegisterPushTokenResponse(r.pushDeviceId(), r.created()));
    }




}