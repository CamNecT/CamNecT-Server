package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PhoneNumbersTest {
    @Test
    void normalizesDomesticAndInternationalFormsWithoutChangingTheNumber() {
        for (String value : new String[]{"01012345678", "010-1234-5678", " 010 1234 5678 ", "+82 10-1234-5678"}) {
            assertThat(PhoneNumbers.normalize(value)).isEqualTo("01012345678");
        }
    }

    @Test
    void rejectsMissingMalformedAndNonKoreanMobileNumbers() {
        for (String value : new String[]{null, "", " ", "0212345678", "+15551234567", "0101234567",
                "010123456789", "010１２３４５６７８", "0101234abc8", "x01012345678"}) {
            assertThatThrownBy(() -> PhoneNumbers.normalize(value)).isInstanceOf(CustomException.class);
        }
    }
}
