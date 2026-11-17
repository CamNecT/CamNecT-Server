package CamNecT.server.domain.activity.ingestion;

import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ActivityImportController {
    private final ActivityImportAuth auth;
    private final ActivityImportService service;
    private final ObjectMapper mapper;
    private final Validator validator;

    @PostMapping(value = ActivityImportAuth.PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "건국대 공지 자동 등록", description = "전용 HMAC 서명이 필요한 크롤러 API. X-Crawler-Timestamp는 UTC Unix 초이며 현재 시각에서 ±300초 이내여야 합니다.",
            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "X-Crawler-Timestamp", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = "[0-9]+")))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivityImportRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수집 데이터 반영 결과", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 JSON·수집 필드 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "40100 HMAC 서명·타임스탬프가 없거나 유효하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "40300 크롤러 수집이 비활성화되었거나 서명 키가 설정되지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "40900 동일 revision의 수집 내용 해시 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "41300 원문 본문이 12,000,000바이트 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 application/json 요청이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<ActivityImportService.Result> ingest(HttpServletRequest request) throws IOException {
        return ingest(request, false);
    }

    @PostMapping(value = ActivityImportAuth.STUDENT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "건국대 학생공지 자동 등록", description = "학생공지 경로 전용 HMAC 서명이 필요합니다. X-Crawler-Timestamp는 UTC Unix 초이며 현재 시각에서 ±300초 이내여야 합니다.",
            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "X-Crawler-Timestamp", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = "[0-9]+")))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivityImportRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "학생공지 반영 결과", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 JSON·수집 필드 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "40100 HMAC 서명·타임스탬프가 없거나 유효하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "40300 크롤러 수집이 비활성화되었거나 서명 키가 설정되지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "40900 동일 revision의 수집 내용 해시 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "41300 원문 본문이 12,000,000바이트 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "41500 application/json 요청이 아님", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<ActivityImportService.Result> ingestStudent(HttpServletRequest request) throws IOException {
        return ingest(request, true);
    }

    private ApiResponse<ActivityImportService.Result> ingest(HttpServletRequest request, boolean student) throws IOException {
        byte[] body = request.getInputStream().readNBytes(12_000_001);
        if (body.length > 12_000_000) throw new CustomException(ErrorCode.PAYLOAD_TOO_LARGE);
        auth.verify(student ? ActivityImportAuth.STUDENT_PATH : ActivityImportAuth.PATH, request.getHeader("X-Crawler-Timestamp"), request.getHeader("X-Crawler-Signature"), body);
        ActivityImportRequest payload;
        try { payload = mapper.readValue(body, ActivityImportRequest.class); }
        catch (IOException exception) { throw new CustomException(ErrorCode.BAD_REQUEST); }
        if (payload == null) throw new CustomException(ErrorCode.BAD_REQUEST);
        var violations = validator.validate(payload);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        return ApiResponse.success(service.ingest(payload, student));
    }
}
