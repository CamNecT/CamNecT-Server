package CamNecT.server.domain.activity.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.activity-import")
public record ActivityImportProperties(boolean enabled, String sharedSecret, Long authorUserId, String authorUsername) {
    public ActivityImportProperties {
        if (authorUsername == null || authorUsername.isBlank()) authorUsername = "camnect";
    }
}
