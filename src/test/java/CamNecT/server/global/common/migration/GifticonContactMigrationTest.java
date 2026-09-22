package CamNecT.server.global.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class GifticonContactMigrationTest {
    @Test
    void addsNullablePhoneSnapshotsWithoutChangingExistingOrders() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE gifticon_purchases (purchase_id BIGINT PRIMARY KEY, buyer_email VARCHAR(255), recipient_email VARCHAR(255), total_price_points INT, export_batch_id BIGINT)");
            statement.execute("INSERT INTO gifticon_purchases VALUES (1, 'buyer@example.com', 'recipient@example.com', 2000, 42)");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V18__Add_gifticon_phone_snapshots.sql"));
            try (var result = statement.executeQuery("SELECT * FROM gifticon_purchases WHERE purchase_id=1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("buyer_phone")).isNull();
                assertThat(result.getString("recipient_phone")).isNull();
                assertThat(result.getString("recipient_email")).isEqualTo("recipient@example.com");
                assertThat(result.getInt("total_price_points")).isEqualTo(2000);
                assertThat(result.getLong("export_batch_id")).isEqualTo(42);
            }
            statement.execute("UPDATE gifticon_purchases SET recipient_phone='01012345678' WHERE purchase_id=1");
        }
    }
}
