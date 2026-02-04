package CamNecT.CamNecT_Server.global.storage.service;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.CamNecT_Server.global.storage.config.PresignProps;
import CamNecT.CamNecT_Server.global.storage.config.S3Props;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignDownloadResponse;
import CamNecT.CamNecT_Server.global.storage.dto.response.PresignUploadResponse;
import CamNecT.CamNecT_Server.global.storage.model.UploadPurpose;
import CamNecT.CamNecT_Server.global.storage.model.UploadRefType;
import CamNecT.CamNecT_Server.global.storage.model.UploadTicket;
import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignEngine {

    private final S3Presigner presigner;
    private final S3Client s3;
    private final S3Props s3Props;
    private final PresignProps presignProps;
    private final UploadTicketRepository ticketRepo;

    private static final String TEMP_ROOT = "temp";

    private static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Transactional
    public PresignUploadResponse issueUpload(Long userId,
                                             UploadPurpose purpose,
                                             String keyPrefix,
                                             String contentType,
                                             long size,
                                             String originalFilename) {
        LocalDateTime now = LocalDateTime.now();

        ticketRepo.bulkExpirePendingByUserPurpose(userId, purpose, now);

        long active = ticketRepo.countByUserIdAndPurposeAndStatusAndExpiresAtAfter(
                userId, purpose, UploadTicket.Status.PENDING, now
        );
        if (active >= 1) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_LIMIT_EXCEEDED);


        String ct = normalize(contentType);
        String ext = EXT_BY_CONTENT_TYPE.getOrDefault(ct, "");

        String tempPrefix = toTempPrefix(keyPrefix); // 첫 업로드는 무조건 temp로 강제

        String key = buildKey(tempPrefix, UUID.randomUUID() + ext);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(presignProps.uploadExpirationSeconds());

        UploadTicket ticket = UploadTicket.builder()
                .userId(userId)
                .purpose(purpose)
                .status(UploadTicket.Status.PENDING)
                .storageKey(key)
                .originalFilename(safeName(originalFilename))
                .contentType(ct)
                .size(size)
                .expiresAt(expiresAt)
                .build();

        ticketRepo.save(ticket);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Props.bucket())
                .key(key)
                .contentType(ct)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignProps.uploadExpirationSeconds()))
                .putObjectRequest(putReq)
                .build();

        String url = presigner.presignPutObject(presignReq).url().toString();

        return new PresignUploadResponse(
                key,
                url,
                expiresAt,
                Map.of("Content-Type", ct)
        );
    }

    /**
     * 검증 + temp->final copy + 커밋/롤백 훅 + USED 처리
     * @return finalKey (DB에는 이걸 저장)
     */
    @Transactional
    public String consume(Long userId,
                          UploadPurpose purpose,
                          UploadRefType refType,
                          Long refId,
                          String tempKey,
                          String finalKeyPrefix) {

        // 1) 티켓/소유/만료/목적 검증 + HEAD 검증
        if (!StringUtils.hasText(tempKey)) throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        if (!StringUtils.hasText(finalKeyPrefix)) throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);

        UploadTicket t = ticketRepo.findByStorageKey(tempKey)
                .orElseThrow(() -> new CustomException(StorageErrorCode.UPLOAD_TICKET_NOT_FOUND));

        if (!Objects.equals(t.getUserId(), userId)) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_FORBIDDEN);
        if (t.getPurpose() != purpose) throw new CustomException(StorageErrorCode.UPLOAD_TICKET_FORBIDDEN);
        if (finalKeyPrefix.startsWith("temp/")) throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        if (!t.isUsable(LocalDateTime.now())) {
            // 만료면 DB도 EXPIRED로 바꿔서 정합성 맞추기
            if (t.getStatus() == UploadTicket.Status.PENDING && t.getExpiresAt().isBefore(LocalDateTime.now())) {
                t.markExpired();
            }
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_EXPIRED_OR_USED);
        }

        HeadObjectResponse head;
        try {
            head = s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Props.bucket())
                    .key(tempKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new CustomException(StorageErrorCode.STORAGE_NOT_FOUND);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new CustomException(StorageErrorCode.STORAGE_NOT_FOUND);
            throw new CustomException(StorageErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }

        if (head.contentLength() != null && head.contentLength() > t.getSize()) {
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_MISMATCHED_OBJECT);
        }
        if (StringUtils.hasText(head.contentType())
                && !normalize(head.contentType()).equals(normalize(t.getContentType()))) {
            throw new CustomException(StorageErrorCode.UPLOAD_TICKET_MISMATCHED_OBJECT);
        }

        String filename = lastSegment(tempKey);
        String finalKey = buildKey(finalKeyPrefix, filename);

        // 2) temp -> final COPY (이 단계에서 temp는 아직 지우지 않음)
        if (!finalKey.equals(tempKey)) {
            try {
                CopyObjectRequest copyReq = CopyObjectRequest.builder()
                        .sourceBucket(s3Props.bucket())
                        .sourceKey(tempKey)
                        .destinationBucket(s3Props.bucket())
                        .destinationKey(finalKey)
                        .build();

                s3.copyObject(copyReq);
            } catch (S3Exception e) {
                throw new CustomException(StorageErrorCode.STORAGE_MOVE_FAILED, e);
            }

            // 3) 트랜잭션 결과에 따라 cleanup
            registerTxCleanup(tempKey, finalKey);

            // 4) 티켓의 storageKey를 final로 갱신
            t.updateStorageKey(finalKey);
        }

        // 5) USED 처리
        t.markUsed(refType, refId);

        return finalKey;
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse presignDownload(String storageKey, String filenameOrNull, String contentTypeOrNull) {

        if (!StringUtils.hasText(storageKey)) {
            throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        }

        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(s3Props.bucket())
                    .key(storageKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new CustomException(StorageErrorCode.STORAGE_NOT_FOUND);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new CustomException(StorageErrorCode.STORAGE_NOT_FOUND);
            throw new CustomException(StorageErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(presignProps.downloadExpirationSeconds());

        GetObjectRequest.Builder getReq = GetObjectRequest.builder()
                .bucket(s3Props.bucket())
                .key(storageKey);

        if (StringUtils.hasText(filenameOrNull)) {
            String encoded = URLEncoder.encode(filenameOrNull, StandardCharsets.UTF_8).replace("+", "%20");
            getReq.responseContentDisposition("attachment; filename*=UTF-8''" + encoded);
        }

        if (StringUtils.hasText(contentTypeOrNull)) {
            getReq.responseContentType(contentTypeOrNull);
        }

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignProps.downloadExpirationSeconds()))
                .getObjectRequest(getReq.build())
                .build();

        String url = presigner.presignGetObject(presignReq).url().toString();
        return new PresignDownloadResponse(url, expiresAt, storageKey);
    }

    private void registerTxCleanup(String tempKey, String finalKey) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        s3.deleteObject(DeleteObjectRequest.builder()
                                .bucket(s3Props.bucket())
                                .key(tempKey)
                                .build());
                    } catch (Exception ignored) {}
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            s3.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(s3Props.bucket())
                                    .key(finalKey)
                                    .build());
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
    }


    private String toTempPrefix(String keyPrefix) {
        String p = trimSlashes(keyPrefix);
        if (p.startsWith(TEMP_ROOT + "/")) return p;
        return TEMP_ROOT + "/" + p;
    }

    private String buildKey(String mid, String filename) {
        String base = trimSlashes(s3Props.prefix());
        String m = trimSlashes(mid);
        return (base + "/" + m + "/" + filename).replaceAll("/+", "/");
    }

    private String lastSegment(String key) {
        String k = trimSlashes(key);
        int idx = k.lastIndexOf('/');
        return (idx >= 0) ? k.substring(idx + 1) : k;
    }

    private String trimSlashes(String s) {
        if (s == null) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String safeName(String name) {
        return StringUtils.hasText(name) ? name : "file";
    }

    private String normalize(String ct) {
        return (ct == null) ? "" : ct.trim().toLowerCase(Locale.ROOT);
    }
}
