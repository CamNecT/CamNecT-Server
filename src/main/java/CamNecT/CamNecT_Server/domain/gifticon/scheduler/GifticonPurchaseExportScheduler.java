package CamNecT.CamNecT_Server.domain.gifticon.scheduler;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonExportBatch;
import CamNecT.CamNecT_Server.domain.gifticon.service.GifticonExportMailService;
import CamNecT.CamNecT_Server.domain.gifticon.service.GifticonExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GifticonPurchaseExportScheduler {

    private final GifticonExportService exportService;
    private final GifticonExportMailService mailService;

    @Scheduled(cron = "${app.gifticon.export-cron:0 10 3 * * *}")
    public void exportPurchases() {
        try {
            GifticonExportBatch batch = exportService.exportRequestedPurchasesToXlsx();
            if (batch != null) {
                log.info("[GifticonPurchaseExportScheduler] exported: {} ({})",
                        batch.getFileName(), batch.getItemCount());

                // export 성공한 파일을 첨부해서 메일 발송
                mailService.sendExportExcel(batch);

            } else {
                log.info("[GifticonPurchaseExportScheduler] no requested purchases");
            }
        } catch (Exception e) {
            log.error("[GifticonPurchaseExportScheduler] export failed", e);
        }
    }
}
