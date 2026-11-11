package CamNecT.server.global.point.service;

import CamNecT.server.global.point.dto.PointHistoryResponse;
import CamNecT.server.global.point.model.TransactionType;
import CamNecT.server.global.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointHistoryService {
    private final PointTransactionRepository transactions;

    public PointHistoryResponse list(Long userId, TransactionType type, Long cursor, int size) {
        var rows = transactions.findHistory(userId, type, cursor, PageRequest.of(0, size + 1));
        var items = rows.stream().limit(size).map(PointHistoryResponse.Item::from).toList();
        return new PointHistoryResponse(items, rows.size() > size ? items.getLast().id() : null);
    }
}
