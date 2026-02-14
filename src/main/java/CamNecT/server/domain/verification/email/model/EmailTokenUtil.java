package CamNecT.server.domain.verification.email.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class EmailTokenUtil {
    private static final SecureRandom RND = new SecureRandom();

    private EmailTokenUtil() {}

    public static String new6DigitCode() {
        int v = RND.nextInt(1_000_000);
        return String.format("%06d", v);
    }

    public static String sha256Hex(String raw){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
