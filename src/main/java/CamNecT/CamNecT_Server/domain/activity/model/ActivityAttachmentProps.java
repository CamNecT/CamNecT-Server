package CamNecT.CamNecT_Server.domain.activity.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.activity.attachments")
public record ActivityAttachmentProps(
        int maxFiles,
        long maxFileSizeMb,
        List<String> allowedContentTypes
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}