package CamNecT.server.global.point.dto;

import CamNecT.server.global.point.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PointHistoryResponse(List<Item> items,
        @Schema(description = "다음 페이지 cursor. 마지막 페이지 또는 빈 목록이면 null") Long nextCursor) {
    @Schema(name = "PointHistoryItem")
    public record Item(Long id, TransactionType type, PointSource source, String description,
                       @Schema(description = "적립은 양수, 사용은 음수", example = "-1000") int pointChange,
                       @Schema(description = "해당 거래 직후 잔액") int balanceAfter, LocalDateTime createdAt) {
        public static Item from(PointTransaction tx) {
            return new Item(tx.getId(), tx.getTransactionType(), tx.getSourceType(),
                    tx.getSourceType().getDescription(), tx.getPointChange(), tx.getBalanceAfter(), tx.getCreatedAt());
        }
    }
}
