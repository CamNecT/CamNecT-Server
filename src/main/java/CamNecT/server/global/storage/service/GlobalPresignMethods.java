package CamNecT.server.global.storage.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.server.global.storage.config.S3Props;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalPresignMethods {

    private final S3Client s3;
    private final S3Props s3Props;
    private final FileStorage fileStorage;

    /** 트랜잭션 커밋 이후에만 삭제(롤백되면 삭제 안 함) */
    public void deleteAfterCommit(Set<String> deleteKeys) {
        if (deleteKeys == null || deleteKeys.isEmpty()) return;

        Set<String> keys = new HashSet<>(deleteKeys);

        Runnable deleteJob = () -> {
            for (String key : keys) {
                if (!StringUtils.hasText(key)) continue;
                try { fileStorage.delete(key); }
                catch (Exception e) { log.warn("Failed to delete file: {}", key, e); }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteJob.run(); }
            });
        } else {
            deleteJob.run();
        }
    }

    public String copyToPrefix(String sourceKey, String destPrefix) {
        if (!StringUtils.hasText(sourceKey) || !StringUtils.hasText(destPrefix)) {
            throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        }

        // sourceKey의 루트(prefix) 추출: "camnect/"
        String base = trimSlashes(s3Props.prefix());
        String src = normalizeKey(sourceKey, base);
        String dstPrefix = normalizePrefix(destPrefix, base);

        String ext = "";
        int slash = src.lastIndexOf('/');
        int dot = src.lastIndexOf('.');
        if (dot > slash) ext = src.substring(dot);

        String destKey = dstPrefix + "/thumb-" + UUID.randomUUID() + ext;
        String bucket = s3Props.bucket();

        try {
            s3.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(src)
                    .destinationBucket(bucket)
                    .destinationKey(destKey)
                    .metadataDirective(MetadataDirective.COPY)
                    .build());
        } catch (S3Exception e) {
            log.warn("copyObject failed. sourceKey={}, destKey={}, status={}, err={}",
                    src, destKey, e.statusCode(),
                    (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : "null"), e);
            throw new CustomException(StorageErrorCode.STORAGE_MOVE_FAILED, e);
        }

        registerRollbackCleanup(destKey);
        return destKey;
    }

    /** Content-Type 정규화: 공백/대소문자/파라미터 제거 */
    public String normalize(String ct) {
        if (ct == null) return "";
        String v = ct.trim().toLowerCase(Locale.ROOT);
        int semi = v.indexOf(';');
        return (semi >= 0) ? v.substring(0, semi).trim() : v;
    }

    private String normalizePrefix(String prefix, String base) {
        String p = prefix.trim();
        if (p.startsWith("/")) p = p.substring(1);
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        p = p.replaceAll("/{2,}", "/");

        String basePrefix = base + "/";
        if (p.startsWith(basePrefix)) p = p.substring(basePrefix.length());
        return basePrefix + p;
    }



    private String normalizeKey(String key, String base) {
        String k = key.trim();
        if (k.startsWith("/")) k = k.substring(1);
        k = k.replaceAll("/{2,}", "/");

        String basePrefix = base + "/";
        // 이미 camnect/...면 그대로, 아니면 camnect/ 붙이기
        if (!k.startsWith(basePrefix)) k = basePrefix + k;

        return k;
    }

    private void registerRollbackCleanup(String keyToDeleteOnRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;

                try {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(s3Props.bucket())
                            .key(keyToDeleteOnRollback)
                            .build());
                } catch (Exception e) {
                    log.warn("rollback cleanup failed. key={}", keyToDeleteOnRollback, e);
                }
            }
        });
    }

    private String trimSlashes(String s) {
        if (s == null) return "";
        String v = s.trim();
        while (v.startsWith("/")) v = v.substring(1);
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }
}
