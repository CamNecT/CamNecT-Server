package CamNecT.CamNecT_Server.domain.gifticon.service;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonExportBatch;
import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonPurchase;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonExportBatchRepository;
import CamNecT.CamNecT_Server.domain.gifticon.repository.GifticonPurchaseRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.GifticonErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GifticonExportService {

    private final GifticonPurchaseRepository purchaseRepository;
    private final GifticonExportBatchRepository batchRepository;

    @Value("${app.gifticon.export-dir:/tmp/gifticon-exports}")
    private String exportDir;

    @Transactional
    public GifticonExportBatch exportRequestedPurchasesToXlsx() {
        List<GifticonPurchase> targets = purchaseRepository.findAllByExportBatchIsNullOrderByRequestedAtAsc();
        if (targets.isEmpty()) return null;

        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "gifticon_purchases_" + ts + ".xlsx";

        try {
            Files.createDirectories(Path.of(exportDir));
            Path filePath = Path.of(exportDir, fileName);

            writeXlsx(filePath, targets);

            GifticonExportBatch batch = batchRepository.save(GifticonExportBatch.builder()
                    .exportedAt(now)
                    .filePath(filePath.toString())
                    .fileName(fileName)
                    .itemCount(targets.size())
                    .build());

            for (GifticonPurchase p : targets) {
                p.markExported(batch, now);
            }

            return batch;

        } catch (Exception e) {
            throw new CustomException(GifticonErrorCode.EXPORT_FAILED);
        }
    }

    private void writeXlsx(Path filePath, List<GifticonPurchase> rows) throws Exception {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("purchases");

            int r = 0;
            Row header = sheet.createRow(r++);
            String[] cols = {
                    "purchaseId", "requestedAt",
                    "userId", "buyerName", "buyerPhone", "buyerEmail",
                    "productId", "vendorProductCode", "brandName", "productName",
                    "unitPricePoints", "quantity", "totalPricePoints",
                    "recipientName", "recipientPhone", "giftMessage"
            };
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            for (GifticonPurchase p : rows) {
                Row row = sheet.createRow(r++);
                int c = 0;

                row.createCell(c++).setCellValue(nvl(p.getId()));
                row.createCell(c++).setCellValue(nvl(p.getRequestedAt()));

                row.createCell(c++).setCellValue(nvl(p.getUser().getUserId()));
                row.createCell(c++).setCellValue(nvl(p.getBuyerName()));
                row.createCell(c++).setCellValue(nvl(p.getBuyerPhone()));
                row.createCell(c++).setCellValue(nvl(p.getBuyerEmail()));

                row.createCell(c++).setCellValue(nvl(p.getProduct().getId()));
                row.createCell(c++).setCellValue(nvl(p.getProduct().getVendorProductCode()));
                row.createCell(c++).setCellValue(nvl(p.getProduct().getBrandName()));
                row.createCell(c++).setCellValue(nvl(p.getProduct().getProductName()));

                row.createCell(c++).setCellValue(nvl(p.getUnitPricePoints()));
                row.createCell(c++).setCellValue(nvl(p.getQuantity()));
                row.createCell(c++).setCellValue(nvl(p.getTotalPricePoints()));

                row.createCell(c++).setCellValue(nvl(p.getRecipientName()));
                row.createCell(c++).setCellValue(nvl(p.getRecipientPhone()));
                row.createCell(c++).setCellValue(nvl(p.getGiftMessage()));
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                wb.write(fos);
            }
        }
    }

    private static String nvl(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}