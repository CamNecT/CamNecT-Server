package CamNecT.server.global.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Props(
        String bucket,
        String region,
        String prefix
) {}
