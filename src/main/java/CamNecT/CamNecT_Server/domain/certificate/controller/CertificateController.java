package CamNecT.CamNecT_Server.domain.certificate.controller;

import CamNecT.CamNecT_Server.domain.certificate.dto.request.CertificateRequest;
import CamNecT.CamNecT_Server.domain.certificate.dto.response.CertificateResponse;
import CamNecT.CamNecT_Server.domain.certificate.service.CertificateService;
import CamNecT.CamNecT_Server.domain.users.model.CustomUserDetails;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CertificateResponse> response = certificateService.getMyCertificate(userDetails.getUserId());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "자격증 정보 추가",
            description = "새로운 자격증 취득 정보를 추가합니다."
    )
    @PostMapping
    public ApiResponse<Void> addCertificate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CertificateRequest request
    ) {
        certificateService.addCertificate(userDetails.getUserId(), request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "자격증 정보 수정",
            description = "기존에 등록된 특정 자격증 정보를 수정합니다."
    )
    @PatchMapping("/{certificateId}")
    public ApiResponse<Void> updateCertificate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long certificateId,
            @RequestBody @Valid CertificateRequest request
    ) {
        certificateService.updateCertificate(userDetails.getUserId(), certificateId, request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "자격증 정보 삭제",
            description = "등록된 자격증 정보 중 하나를 삭제합니다."
    )
    @DeleteMapping("/{certificateId}")
    public ApiResponse<Void> deleteCertificate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long certificateId
    ) {
        certificateService.deleteCertificate(userDetails.getUserId(), certificateId);
        return ApiResponse.success(null);
    }
}