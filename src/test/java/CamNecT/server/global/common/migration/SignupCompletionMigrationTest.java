package CamNecT.server.global.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SignupCompletionMigrationTest {

    @Test
    void preservesExistingHomeUsersWithoutInferringOnboardingFromOptionalFields() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (user_id BIGINT PRIMARY KEY, status VARCHAR(30))");
                statement.execute("CREATE TABLE user_profile (user_id BIGINT PRIMARY KEY, initial_setup_completed BIT NOT NULL, bio VARCHAR(100))");
                statement.execute("INSERT INTO users VALUES (1, 'ACTIVE'), (2, 'ACTIVE'), (3, 'ADMIN_PENDING'), (4, 'SUSPENDED'), (5, 'WITHDRAWN')");
                statement.execute("INSERT INTO user_profile VALUES (1, 1, NULL), (2, 0, 'optional bio'), (3, 1, NULL), (4, 1, NULL), (5, 0, NULL)");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V14__Separate_signup_completion_notification.sql"));

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT initial_setup_completed, verification_complete_notified FROM user_profile ORDER BY user_id")) {
                boolean[][] expected = {{true, true}, {false, false}, {true, false}, {true, true}, {false, false}};
                for (boolean[] flags : expected) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getBoolean(1)).isEqualTo(flags[0]);
                    assertThat(rows.getBoolean(2)).isEqualTo(flags[1]);
                }
                assertThat(rows.next()).isFalse();
            }
            try (var statement = connection.createStatement()) {
                statement.execute("INSERT INTO user_profile (user_id, initial_setup_completed) VALUES (6, 0)");
                try (var rows = statement.executeQuery("SELECT verification_complete_notified FROM user_profile WHERE user_id = 6")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getBoolean(1)).isFalse();
                }
            }
        }
    }
}
