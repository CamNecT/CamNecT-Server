package CamNecT.CamNecT_Server.domain.verification.document.controller;

import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminDocumentVerificationDetailResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminDocumentVerificationListItemResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.CamNecT_Server.domain.verification.document.model.VerificationStatus;
import CamNecT.CamNecT_Server.domain.verification.document.service.AdminDocumentVerificationService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Document Verification", description = "관리자: 문서 인증 심사/조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/verification/documents")
public class AdminDocumentVerificationController {

    private final AdminDocumentVerificationService service;

    @Operation(
            summary = "문서 인증 제출 목록 조회(관리자)",
            description = "상태별(PENDING/APPROVED/REJECTED 등) 제출 목록을 페이징으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(관리자 아님)", content = @Content)
    })
    @GetMapping
    public Page<AdminDocumentVerificationListItemResponse> list(
            @RequestParam(defaultValue = "PENDING") VerificationStatus status,
            Pageable pageable
    ) {
        return service.list(status, pageable);
    }

    @Operation(
            summary = "문서 인증 제출 상세 조회(관리자)",
            description = "submissionId 기준으로 제출 상세를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminDocumentVerificationDetailResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(관리자 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출 없음", content = @Content)
    })
    @GetMapping("/{submissionId}")
    public AdminDocumentVerificationDetailResponse get(@PathVariable Long submissionId) {
        return service.get(submissionId);
    }

    @Operation(
            summary = "문서 인증 심사 처리(관리자)",
            description = "제출 건을 승인/반려 등으로 심사 처리합니다. 성공 시 204를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "심사 처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패/처리 불가 상태", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(관리자 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출 없음", content = @Content)
    })
    @PatchMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(
            @UserId Long adminId,
            @PathVariable Long submissionId,
            @RequestBody @Valid AdminReviewDocumentVerificationRequest req
    ) {
        service.review(adminId, submissionId, req);
    }

    @Operation(
            summary = "문서 다운로드 URL 발급(관리자)",
            description = "제출된 문서 파일을 다운로드할 수 있는 presigned URL을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignDownloadResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(관리자 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출 없음", content = @Content)
    })
    @GetMapping("/{submissionId}/download-url")
    public PresignDownloadResponse downloadUrl(@PathVariable Long submissionId) {
        return service.downloadUrl(submissionId);
    }
}
