package CamNecT.server.domain.gifticon.service;

import CamNecT.server.domain.gifticon.model.GifticonExportBatch;
import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.gifticon.repository.GifticonExportBatchRepository;
import CamNecT.server.domain.gifticon.repository.GifticonPurchaseRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
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
            throw new CustomException(ErrorCode.INTERNAL_ERROR, e);
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

            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            for (GifticonPurchase p : rows) {
                Row row = sheet.createRow(r++);
                int c = 0;

                c = writeCell(row, c, p.getId());
                c = writeCell(row, c, p.getRequestedAt());

                c = writeCell(row, c, p.getUser().getUserId());
                c = writeCell(row, c, p.getBuyerName());
                c = writeCell(row, c, p.getBuyerPhone());
                c = writeCell(row, c, p.getBuyerEmail());

                c = writeCell(row, c, p.getProduct().getId());
                c = writeCell(row, c, p.getProduct().getVendorProductCode());
                c = writeCell(row, c, p.getProduct().getBrandName());
                c = writeCell(row, c, p.getProduct().getProductName());

                c = writeCell(row, c, p.getUnitPricePoints());
                c = writeCell(row, c, p.getQuantity());
                c = writeCell(row, c, p.getTotalPricePoints());

                c = writeCell(row, c, p.getRecipientName());
                c = writeCell(row, c, p.getRecipientPhone());
                writeCell(row, c, p.getGiftMessage());
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                wb.write(fos);
            }
        }
    }

    private int writeCell(Row row, int col, Object value) {
        row.createCell(col).setCellValue(value == null ? "" : String.valueOf(value));
        return col + 1;
    }
}
