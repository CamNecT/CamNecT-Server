package CamNecT.server.domain.verification.phone;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class PhoneCodeHasher {
    private final PhoneVerificationProperties properties;

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getCodeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash phone verification data", e);
        }
    }

    public boolean matches(String expected, String value) {
        return expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), hash(value).getBytes(StandardCharsets.US_ASCII));
    }
}
