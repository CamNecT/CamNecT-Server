package CamNecT.CamNecT_Server.domain.activity.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "app.activity.attachments")
public record ActivityAttachmentProps(
        int maxFiles,
        long maxFileSizeMb,
        List<String> allowedContentTypes
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
    public Set<String> allowedSet() {
        if (allowedContentTypes == null) return Set.of();
        return allowedContentTypes.stream()
                .filter(org.springframework.util.StringUtils::hasText)
                .map(s -> s.trim().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
