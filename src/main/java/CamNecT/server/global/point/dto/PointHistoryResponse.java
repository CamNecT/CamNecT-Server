package CamNecT.server.global.point.dto;

import CamNecT.server.global.point.model.*;
import java.time.LocalDateTime;
import java.util.List;

public record PointHistoryResponse(List<Item> items, Long nextCursor) {
    public record Item(Long id, TransactionType type, PointSource source, String description,
                       int pointChange, int balanceAfter, LocalDateTime createdAt) {
        public static Item from(PointTransaction tx) {
            return new Item(tx.getId(), tx.getTransactionType(), tx.getSourceType(),
                    tx.getSourceType().getDescription(), tx.getPointChange(), tx.getBalanceAfter(), tx.getCreatedAt());
        }
    }
}
