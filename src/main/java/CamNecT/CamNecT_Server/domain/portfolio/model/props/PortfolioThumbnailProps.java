package CamNecT.CamNecT_Server.domain.portfolio.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.portfolio.thumbnail")
public record PortfolioThumbnailProps(
        long maxFileSizeMb
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}