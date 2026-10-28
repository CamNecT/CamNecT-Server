package CamNecT.server.domain.activity.ingestion;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class ActivityImportAuth {
    public static final String PATH = "/api/internal/activity-imports/konkuk-job";
    private final ActivityImportProperties properties;

    public void verify(String timestamp, String signature, byte[] body) {
        if (!properties.enabled() || properties.sharedSecret() == null || properties.sharedSecret().length() < 32)
            throw new CustomException(ErrorCode.FORBIDDEN);
        try {
            long seconds = Long.parseLong(timestamp);
            long current = Instant.now().getEpochSecond();
            if (seconds < current - 300 || seconds > current + 300) throw new IllegalArgumentException();
            String canonical = timestamp + "\nPOST\n" + PATH + "\n" + sha256(body);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.sharedSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            if (signature == null || !signature.matches("[a-f0-9]{64}") ||
                    !MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature)))
                throw new IllegalArgumentException();
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
