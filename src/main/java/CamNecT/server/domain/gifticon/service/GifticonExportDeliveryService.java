package CamNecT.server.domain.gifticon.service;

import CamNecT.server.domain.gifticon.model.GifticonExportBatch;
import CamNecT.server.domain.gifticon.model.GifticonExportDeliveryStatus;
import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.gifticon.repository.GifticonExportBatchRepository;
import CamNecT.server.domain.gifticon.repository.GifticonPurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GifticonExportDeliveryService {

    static final int DUE_BATCH_LIMIT = 20;
    static final int MAX_DELIVERY_ATTEMPTS = 5;
    static final Duration RETRY_DELAY = Duration.ofHours(1);

    private final GifticonExportBatchRepository batchRepository;
    private final GifticonPurchaseRepository purchaseRepository;
    private final GifticonExportFileService fileService;
    private final GifticonExportMailService mailService;
    private final Clock clock;
    private final TransactionTemplate transaction;

    @Value("${app.gifticon.export-mail.delete-after-send:true}")
    private boolean deleteAfterSend;

    @Transactional(readOnly = true)
    public List<Long> findDueReadyBatchIds() {
        return batchRepository.findDueReadyBatchIds(
                GifticonExportDeliveryStatus.READY,
                LocalDateTime.now(clock),
                PageRequest.of(0, DUE_BATCH_LIMIT)
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DeliveryOutcome deliverBatch(Long batchId) {
        Preparation preparation = transaction.execute(status -> prepare(batchId));
        if (preparation.outcome() != null) return preparation.outcome();
        GifticonExportBatch batch = preparation.batch();
        GifticonExportMailResult result;
        try {
            result = mailService.sendExportExcel(batch);
        } catch (RuntimeException e) {
            log.error("[gifticon-export] verify SMTP delivery before retrying batchId={}", batchId, e);
            return DeliveryOutcome.UNCONFIRMED;
        }
        if (result.requiresConfirmation()) {
            log.error("[gifticon-export] verify SMTP delivery before retrying batchId={}", batchId);
            return DeliveryOutcome.UNCONFIRMED;
        }
        return transaction.execute(status -> finish(batch, result));
    }

    private record Preparation(GifticonExportBatch batch, DeliveryOutcome outcome) {}

    private Preparation prepare(Long batchId) {
        LocalDateTime now = LocalDateTime.now(clock);
        GifticonExportBatch batch = batchRepository.findByIdForDelivery(batchId).orElse(null);
        if (batch == null || !batch.isDue(now)) {
            return new Preparation(null, DeliveryOutcome.SKIPPED);
        }

        List<GifticonPurchase> purchases = purchaseRepository.findAllForExportBatch(batchId);
        if (purchases.isEmpty()) {
            return new Preparation(null, recordFailure(batch, now, "BATCH_PURCHASES_EMPTY"));
        }
        if (batch.getItemCount() == null || batch.getItemCount() != purchases.size()) {
            return new Preparation(null, recordFailure(
                    batch,
                    now,
                    "BATCH_ITEM_COUNT_MISMATCH: expected=" + batch.getItemCount()
                            + ", actual=" + purchases.size()
            ));
        }

        Path filePath;
        try {
            filePath = Path.of(batch.getFilePath());
            fileService.ensureFile(filePath, purchases);
        } catch (Exception e) {
            log.error("[gifticon-export] file generation failed batchId={}", batchId, e);
            return new Preparation(null, recordFailure(batch, now, describe("FILE_ERROR", e)));
        }

        batch.beginSubmission(now);
        return new Preparation(batch, null);
    }

    private DeliveryOutcome finish(GifticonExportBatch claimed, GifticonExportMailResult mailResult) {
        GifticonExportBatch batch = batchRepository.findByIdForDelivery(claimed.getId()).orElseThrow();
        if (batch.getDeliveryStatus() != GifticonExportDeliveryStatus.SENDING
                || !batch.getDeliveryAttemptCount().equals(claimed.getDeliveryAttemptCount())) {
            return DeliveryOutcome.SKIPPED;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!mailResult.successful()) {
            return recordFailure(batch, now, mailResult.error());
        }

        batch.markSubmitted(now);
        if (deleteAfterSend) {
            fileService.deleteAfterCommit(Path.of(batch.getFilePath()));
        }
        return DeliveryOutcome.SUBMITTED;
    }

    private DeliveryOutcome recordFailure(
            GifticonExportBatch batch,
            LocalDateTime attemptedAt,
            String error
    ) {
        batch.markDeliveryFailed(
                attemptedAt,
                error,
                MAX_DELIVERY_ATTEMPTS,
                attemptedAt.plus(RETRY_DELAY)
        );
        return batch.getDeliveryStatus() == GifticonExportDeliveryStatus.FAILED
                ? DeliveryOutcome.FAILED
                : DeliveryOutcome.RETRY_SCHEDULED;
    }

    private String describe(String prefix, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return prefix + ": " + exception.getClass().getSimpleName();
        }
        return prefix + ": " + exception.getClass().getSimpleName() + ": " + message;
    }

    public enum DeliveryOutcome {
        SUBMITTED,
        RETRY_SCHEDULED,
        UNCONFIRMED,
        FAILED,
        SKIPPED
    }
}
