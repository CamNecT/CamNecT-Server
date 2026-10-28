package CamNecT.server.global.common.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PHONE_MYSQL_MIGRATION_URL", matches = ".+")
class PhoneVerificationMySqlMigrationTest {
    @Test
    void migratesRealMainSchemaAndPreservesExistingData() throws Exception {
        String url = System.getenv("PHONE_MYSQL_MIGRATION_URL");
        String user = System.getenv("PHONE_MYSQL_USERNAME");
        String password = System.getenv("PHONE_MYSQL_PASSWORD");
        Flyway.configure().dataSource(url, user, password).target("20").load().migrate();
        try (var connection = DriverManager.getConnection(url, user, password);
             var sql = connection.createStatement()) {
            sql.execute("INSERT INTO users (user_id,created_at,updated_at,email,name,password_hash,role,status,username) "
                    + "VALUES (9001,NOW(),NOW(),'existing@example.com','Test','unused','USER','ACTIVE','phone-migration-test')");
            sql.execute("INSERT INTO gifticon_products (product_id,brand_name,created_at,updated_at,image_url,is_active,price_points,product_name,vendor_product_code) "
                    + "VALUES (9001,'Test',NOW(),NOW(),'test',1,1000,'Test','phone-migration-test')");
            sql.execute("INSERT INTO gifticon_purchases (purchase_id,buyer_email,buyer_name,client_request_id,quantity,recipient_email,requested_at,total_price_points,unit_price_points,product_id,user_id) "
                    + "VALUES (9001,'existing@example.com','Test','phone-migration-test',1,'recipient@example.com',NOW(),1000,1000,9001,9001)");
        }
        var flyway = Flyway.configure().dataSource(url, user, password).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("22");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(url, user, password);
             var sql = connection.createStatement()) {
            try (var row = sql.executeQuery("SELECT email,phone_num,phone_verified_at FROM users WHERE user_id=9001")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("existing@example.com");
                assertThat(row.getString(2)).isNull();
                assertThat(row.getTimestamp(3)).isNull();
            }
            try (var row = sql.executeQuery("SELECT recipient_email,buyer_phone,recipient_phone,total_price_points FROM gifticon_purchases WHERE purchase_id=9001")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("recipient@example.com");
                assertThat(row.getString(2)).isNull();
                assertThat(row.getString(3)).isNull();
                assertThat(row.getInt(4)).isEqualTo(1000);
            }
            assertThatThrownBy(() -> sql.execute("UPDATE users SET phone_verified_at=NOW() WHERE user_id=9001"))
                    .isInstanceOf(SQLException.class);
            try (var row = sql.executeQuery("SELECT send_count FROM phone_sms_quotas WHERE quota_key='lock'")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getInt(1)).isZero();
            }
        }
    }
}
