package CamNecT.CamNecT_Server.domain.community.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.community.attachments")
public record CommunityAttachmentProps(
        int maxFiles,
        long maxFileSizeMb,
        List<String> allowedContentTypes
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}