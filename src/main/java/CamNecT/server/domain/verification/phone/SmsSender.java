package CamNecT.server.domain.verification.phone;

public interface SmsSender {
    /** Returns the provider's accepted request ID, not a delivery confirmation. */
    String send(String phone, String code);
}
