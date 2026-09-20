package CamNecT.server.global.common.migration;

import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class SignupPhoneRestorationTest {
    @Test
    void migrationPreservesLegacyMembersAndEnforcesUniqueNonNullPhones() throws Exception {
        try (var c = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
             var s = c.createStatement()) {
            s.execute("CREATE TABLE users (user_id BIGINT PRIMARY KEY, email VARCHAR(255), status VARCHAR(30))");
            s.execute("INSERT INTO users VALUES (1,'old@example.com','ACTIVE'),(2,'other@example.com','ACTIVE')");
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/V19__Restore_signup_phone.sql"));
            try (var r = s.executeQuery("SELECT COUNT(*) FROM users WHERE phone_num IS NULL AND status='ACTIVE'")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getInt(1)).isEqualTo(2);
            }
            s.execute("UPDATE users SET phone_num='01012345678' WHERE user_id=1");
            assertThatThrownBy(() -> s.execute("UPDATE users SET phone_num='01012345678' WHERE user_id=2"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void withdrawalClearsPhoneForAnonymizationAndFutureSignup() {
        var user = Users.builder().phoneNum("01012345678").build();
        user.withdrawAnonymize("탈퇴", "withdrawn", null, UserStatus.WITHDRAWN);
        assertThat(user.getPhoneNum()).isNull();
    }
}
