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
        String ext = "";
        int slash = sourceKey.lastIndexOf('/');
        int dot = sourceKey.lastIndexOf('.');
        if (dot > slash) ext = sourceKey.substring(dot);

        String destKey = destPrefix + "/thumb-" + UUID.randomUUID() + ext;
        String bucket = s3Props.bucket();

        try {
            s3.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(destKey)
                    .metadataDirective(MetadataDirective.COPY)
                    .build());
        } catch (S3Exception e) {
            throw new CustomException(StorageErrorCode.STORAGE_MOVE_FAILED, e); // 에러코드는 상황에 맞게
        }
        // 롤백되면 복사본 제거(찌꺼기 방지)
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
}
