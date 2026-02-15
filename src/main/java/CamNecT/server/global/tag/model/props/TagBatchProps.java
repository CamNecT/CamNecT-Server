package CamNecT.server.global.tag.model.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.tag-batch")
public class TagBatchProps {
    private boolean enabled = true;
    private String cron = "0 15 4 * * *";
}