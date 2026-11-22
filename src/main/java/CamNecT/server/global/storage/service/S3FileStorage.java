package CamNecT.server.global.storage.service;

import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorage implements FileStorage {

    private final S3Client s3;
    private final S3Props props;

    @Override
    public String save(String prefix, MultipartFile file) {
        StorageKeys.validatePrefix(prefix);
        StorageKeys.validateFile(file);

        String safePrefix = StorageKeys.sanitizePrefix(prefix);
        String ext = StorageKeys.resolveExtension(file);
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
        StorageKeys.validateKey(storageKey);

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

    private String buildKey(String subPrefix, String filename) {
        String base = StorageKeys.trimSlashes(props.prefix());
        String mid = StorageKeys.trimSlashes(subPrefix);
        return (base + "/" + mid + "/" + filename).replaceAll("/+", "/");
    }

}