package CamNecT.server.domain.verification.document.controller;

import CamNecT.server.domain.verification.document.dto.AdminDocumentVerificationDetailResponse;
import CamNecT.server.domain.verification.document.dto.AdminDocumentVerificationListItemResponse;
import CamNecT.server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.server.domain.verification.document.model.VerificationStatus;
import CamNecT.server.domain.verification.document.service.AdminDocumentVerificationService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.storage.dto.response.PresignDownloadResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Document Verification", description = "관리자 문서 인증 심사 및 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/verification/documents")
public class AdminDocumentVerificationController {

    private final AdminDocumentVerificationService service;

    @Operation(
            summary = "문서 인증 제출 목록 조회(관리자)",
            description = "상태별 제출 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "40000 status 또는 페이지 요청값 형식 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "40100 유효하지 않거나 만료된 JWT / 41103 인증 헤더 누락·형식 오류 / 41104 토큰 타입 누락 / 41106 허용되지 않은 토큰 타입", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "42402 제출 사용자 정보를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "50000 제출 목록 조회 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            description = "submissionId를 기준으로 제출 상세를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminDocumentVerificationDetailResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "40000 잘못된 submissionId 형식", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "40100 유효하지 않거나 만료된 JWT / 41103 인증 헤더 누락·형식 오류 / 41104 토큰 타입 누락 / 41106 허용되지 않은 토큰 타입", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "42401 제출 / 42402 제출 사용자를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "50000 제출 상세 조회 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{submissionId}")
    public AdminDocumentVerificationDetailResponse get(@PathVariable Long submissionId) {
        return service.get(submissionId);
    }

    @Operation(
            summary = "문서 인증 심사 처리(관리자)",
            description = "제출 건을 승인 또는 반려합니다. 승인 시 사용자 프로필과 상태를 갱신하고 가입 포인트를 지급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "심사 처리 성공"),
            @ApiResponse(responseCode = "400", description = "40000 요청값 형식·검증 오류 / 42011 반려 사유 누락 / 42033 승인 필수 학생 정보 누락", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "40100 유효하지 않거나 만료된 JWT / 41103 인증 헤더 누락·형식 오류 / 41104 토큰 타입 누락 / 41106 허용되지 않은 토큰 타입", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음 / 41301 이메일 인증이 완료되지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "42401 제출 / 42402 사용자 / 44402 사용자 프로필 / 44407 학교 / 44408 전공을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "42910 심사 가능 상태가 아님 / 40900 포인트 처리 중 동시성 충돌", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "41500 지원하지 않는 요청 Content-Type", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "44150 포인트 지갑 생성 실패 / 50000 심사 처리 또는 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            description = "제출 문서를 다운로드할 수 있는 presigned URL을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignDownloadResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "40000 잘못된 submissionId 형식", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "40100 유효하지 않거나 만료된 JWT / 41103 인증 헤더 누락·형식 오류 / 41104 토큰 타입 누락 / 41106 허용되지 않은 토큰 타입", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "40300 관리자 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "42401 제출 / 42403 제출 파일 / 49401 저장 파일을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "49902 저장 파일 확인 실패 / 50000 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{submissionId}/download-url")
    public PresignDownloadResponse downloadUrl(@PathVariable Long submissionId) {
        return service.downloadUrl(submissionId);
    }
}
