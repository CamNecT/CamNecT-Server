package CamNecT.server.domain.verification.phone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneVerificationPropertiesTest {
    @Test
    void defaultRolloutKeepsEmailPurchasesWithoutSmsCredentials() {
        var properties = new PhoneVerificationProperties();
        assertThat(properties.isPhoneFlowActive()).isFalse();
        assertThat(properties.isEnabled()).isFalse();
        assertThatCode(properties::validateCredentials).doesNotThrowAnyException();
    }

    @Test
    void verifiedPhoneCannotStartWithSmsDisabled() {
        var properties = new PhoneVerificationProperties();
        properties.setRolloutMode(PhoneVerificationProperties.RolloutMode.VERIFIED_PHONE);
        assertThatThrownBy(properties::validateCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMS to be enabled");
    }
}
