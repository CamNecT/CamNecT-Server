package CamNecT.server.global.point.controller;

import CamNecT.server.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.response.ApiResponse;
import CamNecT.server.global.point.dto.PointHistoryResponse;
import CamNecT.server.global.point.model.TransactionType;
import CamNecT.server.global.point.service.PointHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointHistoryController {
    private final PointHistoryService history;

    @GetMapping("/transactions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "40000 type은 EARN/SPEND, cursor는 양수, size는 1~100이어야 함 / 쿼리 형식 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Operation(summary = "내 포인트 이용 내역", description = "인증된 사용자의 적립·사용(기프티콘·열람권 구매 포함) 원장을 ID 내림차순으로 조회합니다. type을 생략하면 전체 내역을 반환하므로 프론트에서 각 항목의 type(EARN/SPEND)으로 필터링할 수 있습니다. 선택적으로 서버 type 필터도 지원합니다. 다음 요청의 cursor에는 원본 응답의 nextCursor를 전달합니다. nextCursor가 null이면 마지막 페이지이며, 프론트 필터 결과가 비어 있어도 nextCursor가 있으면 다음 페이지를 조회해야 합니다.")
    public ApiResponse<PointHistoryResponse> list(@UserId Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(history.list(userId, type, cursor, size));
    }
}
