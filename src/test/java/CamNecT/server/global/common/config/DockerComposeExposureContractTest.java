package CamNecT.server.global.common.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeExposureContractTest {

    @Test
    void productionAppIsPublishedOnlyOnLoopback() throws Exception {
        String compose = Files.readString(Path.of("docker-compose.prod.yml"));
        assertThat(compose.lines().map(String::trim).filter(line -> line.contains(":8080")).toList())
                .containsExactly("- \"127.0.0.1:8080:8080\"");
    }
}
