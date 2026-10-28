package CamNecT.server.global.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PhoneVerificationMigrationTest {
    @Test
    void addsUnverifiedPhoneWithoutLosingExistingAccountsOrOrdersAndSeedsBudgetLock() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE users (user_id BIGINT PRIMARY KEY, email VARCHAR(255))");
                sql.execute("INSERT INTO users VALUES (1, 'existing@example.com')");
                sql.execute("CREATE TABLE gifticon_purchases (purchase_id BIGINT PRIMARY KEY, recipient_email VARCHAR(255))");
                sql.execute("INSERT INTO gifticon_purchases VALUES (7, 'recipient@example.com')");
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20__Add_gifticon_phone_verification.sql"));
            try (var sql = connection.createStatement(); var rows = sql.executeQuery(
                    "SELECT email, phone_num, phone_verified_at FROM users WHERE user_id=1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("existing@example.com");
                assertThat(rows.getString(2)).isNull();
                assertThat(rows.getTimestamp(3)).isNull();
            }
            try (var sql = connection.createStatement(); var rows = sql.executeQuery(
                    "SELECT send_count FROM phone_sms_quotas WHERE quota_key='lock'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
            try (var sql = connection.createStatement(); var rows = sql.executeQuery(
                    "SELECT recipient_email FROM gifticon_purchases WHERE purchase_id=7")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("recipient@example.com");
            }
        }
    }
}
