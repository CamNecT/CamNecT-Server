package CamNecT.server.global.notification.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.front.links")
@Getter
@Setter
public class FrontLinkProperties {
    private String fallback;
    private String communityPost;
    private String chatRequest;
    private String chatRoom;
}