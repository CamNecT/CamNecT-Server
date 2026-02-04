package CamNecT.CamNecT_Server.global.common.service;

import CamNecT.CamNecT_Server.global.storage.service.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalPresignMethods {

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

    /** Content-Type 정규화: 공백/대소문자/파라미터 제거 */
    public String normalize(String ct) {
        if (ct == null) return "";
        String v = ct.trim().toLowerCase(Locale.ROOT);
        int semi = v.indexOf(';');
        return (semi >= 0) ? v.substring(0, semi).trim() : v;
    }
}
