package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Phone verification", description = "첫 기프티콘 교환 및 전화번호 변경 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile/me/phone")
public class PhoneVerificationController {
    private final PhoneVerificationService service;

    public record UpdatePhoneRequest(@NotBlank @Size(max = 20) String phoneNum) {}
    public record VerifyPhoneRequest(
            @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String challengeId,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}

    @Operation(summary = "내 전화번호 및 인증 상태")
    @GetMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    public ApiResponse<PhoneVerificationService.PhoneStatus> status(@UserId Long userId) {
        return ApiResponse.success(service.status(userId));
    }

    @Operation(summary = "전화번호 등록·변경", description = "변경 즉시 인증이 해제됩니다. 같은 번호를 다시 저장하면 기존 인증이 유지됩니다.")
    @PutMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전화번호 저장 성공", useReturnTypeSchema = true)
    public ApiResponse<PhoneVerificationService.PhoneStatus> update(
            @UserId Long userId, @RequestBody @Valid UpdatePhoneRequest request) {
        return ApiResponse.success(service.updatePhone(userId, request.phoneNum()));
    }

    @Operation(summary = "저장된 전화번호로 인증번호 발송", description = "ACTIVE 계정만 가능하며, 이미 인증한 번호는 재발송하지 않습니다.")
    @PostMapping("/verification/send")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 접수 성공", useReturnTypeSchema = true)
    public ApiResponse<PhoneVerificationService.SendResponse> send(@UserId Long userId) {
        return ApiResponse.success(service.send(userId));
    }

    @Operation(summary = "전화번호 인증 완료", description = "성공 후 번호가 바뀌지 않는 동안 모든 기프티콘 교환에 인증이 유지됩니다.")
    @PostMapping("/verification/confirm")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공", useReturnTypeSchema = true)
    public ApiResponse<PhoneVerificationService.PhoneStatus> verify(
            @UserId Long userId, @RequestBody @Valid VerifyPhoneRequest request) {
        return ApiResponse.success(service.verify(userId, request.challengeId(), request.code()));
    }
}
