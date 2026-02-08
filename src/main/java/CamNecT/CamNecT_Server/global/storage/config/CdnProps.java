package CamNecT.CamNecT_Server.global.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cdn")
public record CdnProps(String baseUrl) {}