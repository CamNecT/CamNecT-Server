package CamNecT.server.global.storage.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.server.global.storage.config.LocalStorageProps;
import CamNecT.server.global.storage.config.S3Props;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FileStorageContractTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothBackendsPreserveUploadKeysAndValidationErrors(boolean local) {
        FileStorage storage = local ? new LocalFileStorage(new LocalStorageProps(directory.toString()))
                : new S3FileStorage(mock(S3Client.class), new S3Props("bucket", "region", "camnect"));
        var file = new MockMultipartFile("file", "My Image.PNG", "image/png", new byte[]{1});
        String key = storage.save("/Profile/Photos/", file);
        String prefix = local ? "profile/photos/" : "camnect/profile/photos/";
        assertThat(key).startsWith(prefix).endsWith(".png");
        assertThat(assertThrows(CustomException.class, () -> storage.save(" ", file)).getErrorCode())
                .isEqualTo(StorageErrorCode.STORAGE_PREFIX_REQUIRED);
        assertThat(assertThrows(CustomException.class, () -> storage.save("profile", null)).getErrorCode())
                .isEqualTo(StorageErrorCode.STORAGE_EMPTY_FILE);
        assertThat(assertThrows(CustomException.class, () -> storage.delete(" ")).getErrorCode())
                .isEqualTo(StorageErrorCode.STORAGE_KEY_REQUIRED);
        storage.delete(key);
        if (local) assertThat(Files.exists(directory.resolve(key))).isFalse();
    }
}
