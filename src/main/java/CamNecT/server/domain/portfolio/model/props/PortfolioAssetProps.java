package CamNecT.server.domain.portfolio.model.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.portfolio.assets")
public record PortfolioAssetProps(
        int maxFiles,
        long maxFileSizeMb,
        List<String> allowedContentTypes
) {
    public long maxFileSizeBytes() { return maxFileSizeMb * 1024L * 1024L; }
}