package CamNecT.server.domain.activity.ingestion;

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
    @io.swagger.v3.oas.annotations.Operation(summary = "건국대 공지 자동 등록", description = "전용 HMAC 서명이 필요한 크롤러 API")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수집 데이터 반영 결과", useReturnTypeSchema = true)
    public ApiResponse<ActivityImportService.Result> ingest(HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readNBytes(12_000_001);
        if (body.length > 12_000_000) throw new CustomException(ErrorCode.PAYLOAD_TOO_LARGE);
        auth.verify(request.getHeader("X-Crawler-Timestamp"), request.getHeader("X-Crawler-Signature"), body);
        ActivityImportRequest payload;
        try { payload = mapper.readValue(body, ActivityImportRequest.class); }
        catch (IOException exception) { throw new CustomException(ErrorCode.BAD_REQUEST); }
        if (payload == null) throw new CustomException(ErrorCode.BAD_REQUEST);
        var violations = validator.validate(payload);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        return ApiResponse.success(service.ingest(payload));
    }
}
