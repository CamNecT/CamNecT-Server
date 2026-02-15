package CamNecT.server.domain.profile.components.certificate.controller;

import CamNecT.server.domain.profile.components.certificate.dto.request.CertificateRequest;
import CamNecT.server.domain.profile.components.certificate.dto.response.CertificateResponse;
import CamNecT.server.domain.profile.components.certificate.service.CertificateService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Certificate", description = "자격증 정보 관련 API")
@RestController
@RequestMapping("/api/user/me/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(
            summary = "내 자격증 목록 조회",
            description = "현재 로그인한 사용자가 등록한 모든 자격증 리스트를 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<CertificateResponse>> getMyCertificates(
            @UserId Long userId
    ) {
        List<CertificateResponse> response = certificateService.getMyCertificate(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "자격증 정보 추가",
            description = "새로운 자격증 취득 정보를 추가합니다."
    )
    @PostMapping
    public ApiResponse<Void> addCertificate(
            @UserId Long userId,
            @RequestBody @Valid CertificateRequest request
    ) {
        certificateService.addCertificate(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "자격증 정보 수정",
            description = "기존에 등록된 특정 자격증 정보를 수정합니다."
    )
    @PatchMapping("/{certificateId}")
    public ApiResponse<Void> updateCertificate(
            @UserId Long userId,
            @PathVariable Long certificateId,
            @RequestBody @Valid CertificateRequest request
    ) {
        certificateService.updateCertificate(userId, certificateId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "자격증 정보 삭제",
            description = "등록된 자격증 정보 중 하나를 삭제합니다."
    )
    @DeleteMapping("/{certificateId}")
    public ApiResponse<Void> deleteCertificate(
            @UserId Long userId,
            @PathVariable Long certificateId
    ) {
        certificateService.deleteCertificate(userId, certificateId);
        return ApiResponse.success(null);
    }
}