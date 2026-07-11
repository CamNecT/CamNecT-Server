package CamNecT.server.global.storage.service;

import CamNecT.server.global.storage.config.LocalStorageProps;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    private final LocalStorageProps props;

    @Override
    public String save(String prefix, MultipartFile file) {
        validatePrefix(prefix);
        validateFile(file);

        Path base = baseDir();
        Path dir = base.resolve(sanitizePrefix(prefix)).normalize();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new CustomException(StorageErrorCode.STORAGE_UPLOAD_FAILED, e);
        }

        String ext = resolveExtension(file);
        String filename = UUID.randomUUID() + ext;

        Path target = dir.resolve(filename).normalize();
        assertUnderBase(base, target);

        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CustomException(StorageErrorCode.STORAGE_UPLOAD_FAILED, e);
        }

        // DB에는 baseDir 제외한 "상대 key" 저장
        // prefix/uuid.ext 형태로 반환
        String safePrefix = sanitizePrefix(prefix);
        return (safePrefix + "/" + filename).replaceAll("/+", "/");
    }

    @Override
    public void delete(String storageKey) {
        validateKey(storageKey);

        Path base = baseDir();
        Path path = base.resolve(sanitizeKey(storageKey)).normalize();
        assertUnderBase(base, path);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new CustomException(StorageErrorCode.STORAGE_DELETE_FAILED, e);
        }
    }

    // =======================
    // 내부 유틸
    // =======================

    private Path baseDir() {
        String baseDir = props.baseDir();
        if (!StringUtils.hasText(baseDir)) {
            // base-dir 설정 누락은 서버 설정 오류이므로 500 계열로 보는 편이 일반적
            throw new CustomException(StorageErrorCode.STORAGE_UPLOAD_FAILED);
        }
        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    private void validatePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            throw new CustomException(StorageErrorCode.STORAGE_PREFIX_REQUIRED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(StorageErrorCode.STORAGE_EMPTY_FILE);
        }
    }

    private void validateKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        }
    }

    /**
     * prefix는 서버가 만든 값만 넣는 전제 (예: "verification/123", "profiles/10")
     */
    private String sanitizePrefix(String prefix) {
        String p = prefix.replace("\\", "/").toLowerCase(Locale.ROOT);
        p = trimSlashes(p);
        if (p.contains("..")) {
            throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        }
        return p;
    }

    /**
     * storageKey는 DB에 저장된 값(상대경로). 안전하게 normalize하기 전 최소 방어.
     */
    private String sanitizeKey(String storageKey) {
        String k = storageKey.replace("\\", "/");
        k = trimSlashes(k);
        if (k.contains("..")) {
            throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        }
        return k;
    }

    private void assertUnderBase(Path base, Path target) {
        if (!target.startsWith(base)) {
            throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) return "";
        String ext = StringUtils.getFilenameExtension(original);
        if (!StringUtils.hasText(ext)) return "";
        String safe = ext.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return safe.isEmpty() ? "" : "." + safe;
    }

    private String trimSlashes(String s) {
        if (s == null) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}