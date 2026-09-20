package CamNecT.server.global.common.migration;

import CamNecT.server.domain.community.dto.request.CommunityRequestLimits;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class CommentContentMigrationTest {
    @Test
    void preservesCommentsAndAllowsFullLengthMultibyteContentOnCreateAndUpdate() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE comments (comment_id BIGINT PRIMARY KEY, content TINYTEXT NOT NULL)");
                statement.execute("INSERT INTO comments VALUES (1, '기존 댓글')");
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V20__Expand_comment_content.sql"));
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT content FROM comments WHERE comment_id = 1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("기존 댓글");
            }
            int id = 2;
            for (String body : List.of("가".repeat(85), "가".repeat(86), "가".repeat(CommunityRequestLimits.MAX_COMMENT_CONTENT_LENGTH),
                    "😀".repeat(CommunityRequestLimits.MAX_COMMENT_CONTENT_LENGTH / 2))) {
                try (var insert = connection.prepareStatement("INSERT INTO comments VALUES (?, ?)")) {
                    insert.setInt(1, id++);
                    insert.setString(2, body);
                    insert.executeUpdate();
                }
                try (var update = connection.prepareStatement("UPDATE comments SET content = ? WHERE comment_id = 1")) {
                    update.setString(1, body);
                    update.executeUpdate();
                }
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT content FROM comments WHERE comment_id = 1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo(body);
                }
            }
        }
    }
}
