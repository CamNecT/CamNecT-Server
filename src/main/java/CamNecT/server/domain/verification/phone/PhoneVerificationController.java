package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    })
    public ApiResponse<PhoneVerificationService.PhoneStatus> status(@UserId Long userId) {
        return ApiResponse.success(service.status(userId));
    }

    @Operation(summary = "마이페이지 전화번호 등록·변경", description = "첫 등록 또는 번호 변경 시 미인증으로 저장하며 문자는 보내지 않습니다. 기프티콘 교환 전에 별도 발송·확인 API로 인증합니다. 같은 정규화 번호를 다시 저장하면 기존 인증을 유지합니다.")
    @PutMapping
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전화번호 저장 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청 필드 검증 실패 / 42040 올바르지 않은 국내 휴대전화 번호", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "41304 활성 계정이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<PhoneVerificationService.PhoneStatus> update(
            @UserId Long userId, @RequestBody @Valid UpdatePhoneRequest request) {
        return ApiResponse.success(service.updatePhone(userId, request.phoneNum()));
    }

    @Operation(summary = "저장된 전화번호로 인증번호 발송", description = "ACTIVE 계정만 가능하며, 이미 인증한 번호는 재발송하지 않습니다.")
    @PostMapping("/verification/send")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 접수 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "42041 전화번호 미등록 / 42042 발송 중 번호·인증 요청이 변경됨", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "41304 활성 계정이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "42940 이미 인증한 전화번호", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "42941 재발송 대기 또는 발송 한도 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "42541 SMS 발송 실패 또는 결과 불명", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "42540 문자 인증 기능을 사용할 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<PhoneVerificationService.SendResponse> send(@UserId Long userId) {
        return ApiResponse.success(service.send(userId));
    }

    @Operation(summary = "전화번호 인증 완료", description = "성공 후 번호가 바뀌지 않는 동안 모든 기프티콘 교환에 인증이 유지됩니다.")
    @PostMapping("/verification/confirm")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 요청 필드 검증 실패 / 42042 유효하지 않거나 만료된 인증 요청 / 42043 인증번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "41304 활성 계정이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "42942 인증번호 시도 횟수 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "42540 문자 인증 기능을 사용할 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<PhoneVerificationService.PhoneStatus> verify(
            @UserId Long userId, @RequestBody @Valid VerifyPhoneRequest request) {
        return ApiResponse.success(service.verify(userId, request.challengeId(), request.code()));
    }
}
