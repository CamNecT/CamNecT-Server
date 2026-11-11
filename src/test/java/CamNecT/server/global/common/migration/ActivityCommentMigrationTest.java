package CamNecT.server.global.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityCommentMigrationTest {
    @Test
    void createsTablesWithParentAndLikeConstraints() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE external_activities (activity_id BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO external_activities VALUES (1)");
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V23__Add_activity_comments.sql"));
            try (var statement = connection.createStatement()) {
                statement.execute("INSERT INTO activity_comments (activity_id, user_id, content, status, created_at, updated_at) VALUES (1, 1, '댓글', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
                statement.execute("INSERT INTO activity_comment_likes (comment_id, user_id, created_at) VALUES (1, 1, CURRENT_TIMESTAMP)");
                assertThatThrownBy(() -> statement.execute("INSERT INTO activity_comment_likes (comment_id, user_id, created_at) VALUES (1, 1, CURRENT_TIMESTAMP)"))
                        .isInstanceOf(java.sql.SQLException.class);
                try (var rows = statement.executeQuery("SELECT content FROM activity_comments")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("댓글");
                }
            }
        }
    }
}
