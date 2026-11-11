package CamNecT.server.global.point.controller;

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
    @Operation(summary = "내 포인트 이용 내역", description = "최신순. type을 생략하면 전체, EARN은 적립, SPEND는 사용입니다. 다음 요청의 cursor에는 nextCursor를 전달합니다.")
    public ApiResponse<PointHistoryResponse> list(@UserId Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(history.list(userId, type, cursor, size));
    }
}
