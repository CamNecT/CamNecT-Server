package CamNecT.server.domain.gifticon.service;

import CamNecT.server.domain.gifticon.model.GifticonExportBatch;
import CamNecT.server.domain.gifticon.model.GifticonExportDeliveryStatus;
import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.gifticon.repository.GifticonExportBatchRepository;
import CamNecT.server.domain.gifticon.repository.GifticonPurchaseRepository;
import CamNecT.server.global.common.config.QuerydslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GifticonExportConfirmationIntegrationTest {
    @Autowired GifticonExportBatchRepository batches;
    @Autowired PlatformTransactionManager manager;

    @Test
    void finalizationRollbackNeverResubmitsAcceptedMail() {
        var purchases = mock(GifticonPurchaseRepository.class);
        var files = mock(GifticonExportFileService.class);
        var mail = mock(GifticonExportMailService.class);
        var service = new GifticonExportDeliveryService(batches, purchases, files, mail,
                Clock.systemUTC(), new TransactionTemplate(manager));
        ReflectionTestUtils.setField(service, "deleteAfterSend", true);
        var batch = batches.saveAndFlush(GifticonExportBatch.builder().exportedAt(LocalDateTime.now())
                .fileName("confirmation.xlsx").filePath("confirmation.xlsx").itemCount(1).build());
        Long id = batch.getId();
        try {
            when(purchases.findAllForExportBatch(id)).thenReturn(List.of(mock(GifticonPurchase.class)));
            when(mail.sendExportExcel(any())).thenAnswer(call -> {
                assertThat(batches.findById(id).orElseThrow().getDeliveryStatus())
                        .isEqualTo(GifticonExportDeliveryStatus.SENDING);
                return GifticonExportMailResult.delivered();
            });
            doThrow(new IllegalStateException("finalization failed")).when(files).deleteAfterCommit(any());

            assertThatThrownBy(() -> service.deliverBatch(id)).hasMessage("finalization failed");
            assertThat(batches.findById(id).orElseThrow().getDeliveryStatus()).isEqualTo(GifticonExportDeliveryStatus.SENDING);
            assertThat(service.deliverBatch(id)).isEqualTo(GifticonExportDeliveryService.DeliveryOutcome.SKIPPED);
            verify(mail).sendExportExcel(any());
        } finally {
            batches.deleteById(id);
        }
    }
}
