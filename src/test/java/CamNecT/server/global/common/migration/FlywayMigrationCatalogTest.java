package CamNecT.server.global.common.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationCatalogTest {

    @Test
    void flywayResolvesTheProductionCatalogWithoutDuplicateVersions() {
        // info() uses Flyway's actual resolver but executes no MySQL migration
        // SQL against H2. Local/test profiles otherwise disable Flyway entirely.
        MigrationInfo[] migrations = Flyway.configure()
                .dataSource("jdbc:h2:mem:catalog-" + UUID.randomUUID(), "sa", "")
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load().info().all();

        assertThat(Arrays.stream(migrations).map(MigrationInfo::getVersion).toList())
                .isNotEmpty().doesNotHaveDuplicates();
    }
}
