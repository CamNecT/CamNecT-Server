package CamNecT.server.domain.verification.phone;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs the same HTTP, expiry and concurrent quota tests against a disposable MySQL database. */
@EnabledIfEnvironmentVariable(named = "PHONE_MYSQL_URL", matches = ".+")
class PhoneVerificationMySqlIntegrationTest extends PhoneVerificationIntegrationTest {
    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("PHONE_MYSQL_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("PHONE_MYSQL_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("PHONE_MYSQL_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}
