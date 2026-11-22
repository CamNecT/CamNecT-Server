package CamNecT.server.global.notification.controller;

import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.notification.service.PushPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/notification-preferences")
@RequiredArgsConstructor
public class PushPreferenceController {
    private final PushPreferenceService preferences;

    public record Preference(@NotNull Boolean pushEnabled) {}

    @GetMapping
    @Operation(summary = "내 계정의 전체 푸시 알림 설정 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "현재 푸시 설정", useReturnTypeSchema = true)
    public ApiResponse<Preference> get(@UserId Long userId) {
        return ApiResponse.success(new Preference(preferences.isEnabled(userId)));
    }

    @PatchMapping
    @Operation(summary = "내 모든 기기의 푸시 알림 설정 변경")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장된 푸시 설정", useReturnTypeSchema = true)
    public ApiResponse<Preference> update(@UserId Long userId, @Valid @RequestBody Preference preference) {
        preferences.setEnabled(userId, preference.pushEnabled());
        return ApiResponse.success(preference);
    }
}
