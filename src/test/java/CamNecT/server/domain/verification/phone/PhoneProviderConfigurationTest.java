package CamNecT.server.domain.verification.phone;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PhoneProviderConfigurationTest {
    @Test
    void solapiNeedsOnlyItsOwnCredentialsAndAnIndependentOtpSecret() {
        var p = new PhoneVerificationProperties();
        p.setEnabled(true);
        p.setSenderNumber("01000000000");
        p.setCodeSecret("a-separate-test-otp-secret-at-least-32-characters");
        p.setAccessKey("sens-access");
        p.setSecretKey("sens-secret");
        p.setServiceId("sens-service");
        assertThatThrownBy(p::validateCredentials).isInstanceOf(IllegalStateException.class);
        p.setSolapiApiKey("solapi-key");
        p.setSolapiApiSecret("solapi-secret");
        p.setAccessKey("");
        p.setSecretKey("");
        p.setServiceId("");
        assertThatCode(p::validateCredentials).doesNotThrowAnyException();
        p.setCodeSecret("short");
        assertThatThrownBy(p::validateCredentials).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void monthlyBudgetCannotExceedApprovedFiftyAttemptsEvenWhenDisabled() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var p = new PhoneVerificationProperties();
            assertThat(factory.getValidator().validate(p)).isEmpty();
            p.setMonthlyTotal(51);
            assertThat(factory.getValidator().validate(p))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("monthlyTotal"));
        }
    }
}
