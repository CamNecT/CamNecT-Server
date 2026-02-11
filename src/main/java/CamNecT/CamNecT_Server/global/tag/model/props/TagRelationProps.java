package CamNecT.CamNecT_Server.global.tag.model.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.tag-relation")
public class TagRelationProps {
    private int topK = 15;
    private int minEvidence = 2;
    private double scoreThreshold = 0.15;
    private double alpha = 0.6;
}