package CamNecT.server.global.storage.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

final class StorageKeys {
    private StorageKeys() {}

    static void validatePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            throw new CustomException(StorageErrorCode.STORAGE_PREFIX_REQUIRED);
        }
    }

    static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(StorageErrorCode.STORAGE_EMPTY_FILE);
        }
    }

    static void validateKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        }
    }

    static String sanitizePrefix(String prefix) {
        String p = prefix.replace("\\", "/").toLowerCase(Locale.ROOT);
        p = trimSlashes(p);
        if (p.contains("..")) {
            throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        }
        return p;
    }

    static String trimSlashes(String s) {
        if (s == null) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    static String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) return "";
        String ext = StringUtils.getFilenameExtension(original);
        if (!StringUtils.hasText(ext)) return "";
        String safe = ext.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return safe.isEmpty() ? "" : "." + safe;
    }
}
