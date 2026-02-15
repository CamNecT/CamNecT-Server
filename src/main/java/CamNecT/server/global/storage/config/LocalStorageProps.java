package CamNecT.server.global.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.local")
public record LocalStorageProps(String baseDir) {}