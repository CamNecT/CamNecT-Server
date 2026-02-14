package CamNecT.server.domain.verification.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.verification.document")
public class DocumentVerificationProperties {

    public enum StorageType { LOCAL, S3 }

    private StorageType storage = StorageType.S3;

    private int maxFileSizeMb = 10;
    private int maxFiles = 3;
    private List<String> allowedContentTypes = new ArrayList<>();
    private String storageDir = "./uploads/verifications";

    public long maxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }
}
