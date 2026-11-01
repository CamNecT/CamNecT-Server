package CamNecT.server.domain.activity.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.activity-import.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class ActivityImportExpiryScheduler {
    private final ActivityImportProperties properties;
    private final ActivityImportRepository imports;
    private final ActivityImportService service;

    @Scheduled(fixedDelayString = "${app.activity-import.expiry-delay-ms:60000}", initialDelay = 60000)
    public void expire() {
        if (!properties.enabled()) return;
        Instant now = Instant.now();
        String after = "";
        int closed = 0;
        while (true) {
            var ids = imports.findExpiredCandidates(after, now, now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),
                    PageRequest.of(0, 100));
            if (ids.isEmpty()) break;
            for (String id : ids) {
                try {
                    if (service.expireImported(id, now)) closed++;
                } catch (RuntimeException exception) {
                    log.error("Imported activity expiry failed for {}", id, exception);
                }
            }
            // Keyset paging advances even when a manually edited record is skipped.
            after = ids.getLast();
        }
        if (closed > 0) log.info("Expired {} imported activities", closed);
    }
}
