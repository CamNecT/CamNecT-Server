package CamNecT.CamNecT_Server.domain.verification.document.controller;

import CamNecT.CamNecT_Server.domain.verification.document.dto.DocumentVerificationDetailResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.DocumentVerificationListItemResponse;
import CamNecT.CamNecT_Server.domain.verification.document.dto.SubmitDocumentVerificationRequest;
import CamNecT.CamNecT_Server.domain.verification.document.dto.SubmitDocumentVerificationResponse;
import CamNecT.CamNecT_Server.domain.verification.document.service.DocumentVerificationService;
import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.storage.dto.request.PresignUploadRequest;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Document Verification", description = "사용자: 문서 인증 업로드/제출/조회/취소")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verification/documents")
public class DocumentVerificationController {

    private final DocumentVerificationService service;

    @Operation(
            summary = "업로드용 presigned URL 발급",
            description = "클라이언트가 S3 등에 업로드할 수 있도록 presigned URL을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignUploadResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping("/uploads/presign")
    public PresignUploadResponse presignUpload(
            @UserId Long userId,
            @RequestBody @Valid PresignUploadRequest req
    ) {
        return service.presignUpload(userId, req);
    }

    @Operation(
            summary = "문서 인증 제출",
            description = "업로드된 문서 키(documentKey)를 기반으로 인증 제출을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "제출 성공",
                    content = @Content(schema = @Schema(implementation = SubmitDocumentVerificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패/제출 불가", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitDocumentVerificationResponse submit(
            @UserId Long userId,
            @RequestBody @Valid SubmitDocumentVerificationRequest req
    ) {
        return service.submit(userId, req.docType(), req.documentKey());
    }

    @Operation(
            summary = "내 문서 인증 제출 목록 조회",
            description = "현재 로그인한 사용자의 제출 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DocumentVerificationListItemResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @GetMapping("/me")
    public List<DocumentVerificationListItemResponse> mySubmissions(@UserId Long userId) {
        return service.mySubmissions(userId);
    }

    @Operation(
            summary = "내 문서 인증 제출 상세 조회",
            description = "submissionId 기준으로 내 제출 상세를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DocumentVerificationDetailResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출 없음", content = @Content)
    })
    @GetMapping("/{submissionId}")
    public DocumentVerificationDetailResponse mySubmissionDetail(
            @UserId Long userId,
            @PathVariable Long submissionId
    ) {
        return service.mySubmissionDetail(userId, submissionId);
    }

    @Operation(
            summary = "내 제출 파일 다운로드 URL 발급",
            description = "특정 제출 건의 파일을 다운로드할 수 있는 presigned URL을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignDownloadResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출/파일 없음", content = @Content)
    })
    @GetMapping("/{submissionId}/files/{fileId}/download-url")
    public PresignDownloadResponse myDownloadUrl(
            @UserId Long userId,
            @PathVariable Long submissionId
    ) {
        return service.myDownloadUrl(userId, submissionId);
    }

    @Operation(
            summary = "문서 인증 제출 취소",
            description = "submissionId 기준으로 제출을 취소합니다. 성공 시 204를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가 상태", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 제출 없음", content = @Content)
    })
    @DeleteMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @UserId Long userId,
            @PathVariable Long submissionId
    ) {
        service.cancel(userId, submissionId);
    }
}
