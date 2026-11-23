package CamNecT.server.global.common.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentWorkflowContractTest {

    @Test
    void releaseRequiresBothIndependentJobs() throws Exception {
        try (var reader = Files.newBufferedReader(Path.of(".github/workflows/deploy.yml"))) {
            Map<String, Map<String, Map<String, Object>>> workflow = new Yaml().load(reader);
            var jobs = workflow.get("jobs");

            assertThat(jobs.get("test")).doesNotContainKey("needs");
            assertThat(jobs.get("test").get("steps").toString())
                    .contains("./gradlew test", "python3 -m unittest");
            assertThat(jobs.get("build")).doesNotContainKey("needs");
            assertThat(jobs.get("build").get("steps").toString())
                    .contains("docker/build-push-action").doesNotContain("appleboy/ssh-action", ":latest");
            assertThat(jobs.get("release").get("needs"))
                    .isEqualTo(java.util.List.of("test", "build"));
            assertThat(jobs.get("release").get("steps").toString())
                    .contains("imagetools create", "appleboy/scp-action", "appleboy/ssh-action");
        }
    }
}
