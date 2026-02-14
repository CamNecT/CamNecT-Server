package CamNecT.server.global.storage.service;

import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorage implements FileStorage {

    private final S3Client s3;
    private final S3Props props;

    @Override
    public String save(String prefix, MultipartFile file) {
        validatePrefix(prefix);
        validateFile(file);

        String safePrefix = sanitizePrefix(prefix);
        String ext = resolveExtension(file);
        String key = buildKey(safePrefix, UUID.randomUUID() + ext);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (var in = file.getInputStream()) {
            s3.putObject(req, RequestBody.fromInputStream(in, file.getSize()));
            return key; // DB에는 key 저장
        } catch (IOException | S3Exception e) {
            throw new CustomException(StorageErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        validateKey(storageKey);

        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(props.bucket())
                .key(storageKey)
                .build();

        try {
            s3.deleteObject(req);
        } catch (S3Exception e) {
            throw new CustomException(StorageErrorCode.STORAGE_DELETE_FAILED, e);
        }
    }

    ///////////////// 내부 함수들 /////////////////
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

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) return "";
        String ext = StringUtils.getFilenameExtension(original);
        if (!StringUtils.hasText(ext)) return "";
        String safe = ext.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return safe.isEmpty() ? "" : "." + safe;
    }

    private String buildKey(String subPrefix, String filename) {
        String base = trimSlashes(props.prefix());
        String mid = trimSlashes(subPrefix);
        return (base + "/" + mid + "/" + filename).replaceAll("/+", "/");
    }

    private String sanitizePrefix(String prefix) {
        String p = prefix.replace("\\", "/").toLowerCase(Locale.ROOT);
        p = trimSlashes(p);
        if (p.contains("..")) {
            throw new CustomException(StorageErrorCode.STORAGE_INVALID_PREFIX);
        }
        return p;
    }

    private String trimSlashes(String s) {
        if (s == null) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}