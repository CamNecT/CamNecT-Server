package CamNecT.server.global.common.migration;

import CamNecT.server.global.common.util.StringListConverter;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTechStackMigrationTest {
    @Test
    void preservesExistingRowsAndStoresTheMaximumAllowedTechStack() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE portfolio_project (portfolio_id BIGINT PRIMARY KEY, tech_stack VARCHAR(255) NOT NULL)");
                statement.execute("INSERT INTO portfolio_project VALUES (1, 'Java,Spring'), (2, '')");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V17__Expand_portfolio_tech_stack.sql"));

            var converter = new StringListConverter();
            var skills = Collections.nCopies(10, "가".repeat(50));
            String encoded = converter.convertToDatabaseColumn(skills);
            assertThat(encoded).hasSize(509);
            try (var statement = connection.prepareStatement("INSERT INTO portfolio_project VALUES (3, ?)")) {
                statement.setString(1, encoded);
                statement.executeUpdate();
            }

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT tech_stack FROM portfolio_project ORDER BY portfolio_id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("Java,Spring");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEmpty();
                assertThat(rows.next()).isTrue();
                assertThat(converter.convertToEntityAttribute(rows.getString(1))).containsExactlyElementsOf(skills);
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
