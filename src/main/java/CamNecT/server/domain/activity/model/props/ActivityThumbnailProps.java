package CamNecT.server.domain.activity.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.activity.thumbnail")
public record ActivityThumbnailProps(
        long maxFileSizeMb
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}