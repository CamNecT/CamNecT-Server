package CamNecT.CamNecT_Server.domain.activity.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.activity.attachments")
public record ActivityAttachmentProps(
        int maxFiles,
        long maxFileSizeMb
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}
