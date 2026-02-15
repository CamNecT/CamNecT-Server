package CamNecT.server.global.storage.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DownloadUrlIssuer {

    private final PresignEngine presignEngine;

    @Transactional(readOnly = true)
    public String issueDisplayUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return null;

        try {
            // filename/contentType null -> Content-Disposition 강제 안 걸려서 이미지 표시용에 적합
            return presignEngine.presignDownload(storageKey, null, null).downloadUrl();
        } catch (CustomException e) {
            // 이미지가 없으면 프로필 조회 자체가 터지지 않게 null 처리 (권장)
            if (e.getErrorCode() == StorageErrorCode.STORAGE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}