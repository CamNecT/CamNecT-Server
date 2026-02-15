package CamNecT.server.domain.gifticon.scheduler;

import CamNecT.server.domain.gifticon.service.GifticonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "app.gifticon.vendor", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class GifticonCatalogSyncScheduler {

    private final GifticonService gifticonService;

    @Scheduled(cron = "${app.gifticon.catalog-sync-cron:0 0 3 * * *}")
    public void syncCatalog() {
        try {
            gifticonService.syncCatalogFromVendor();
        } catch (Exception e) {
            log.error("[GifticonCatalogSyncScheduler] sync failed", e);
        }
    }
}