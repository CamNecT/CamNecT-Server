package CamNecT.server.global.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({S3Props.class, LocalStorageProps.class, StorageProperties.class})
public class StorageConfig {

    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Props props) {
        return S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}