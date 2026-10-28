package CamNecT.server.domain.verification.phone;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties("app.phone-verification")
public class PhoneVerificationProperties {
    public enum RolloutMode { LEGACY, VERIFIED_PHONE }
    public enum Provider { SOLAPI, SENS }
    private Provider provider = Provider.SOLAPI;
    private RolloutMode rolloutMode = RolloutMode.LEGACY;
    private boolean enabled = false;
    private String accessKey = "";
    private String secretKey = "";
    private String serviceId = "";
    private String solapiApiKey = "";
    private String solapiApiSecret = "";
    private String senderNumber = "";
    private String codeSecret = "";
    @Min(30) private int expirationSeconds = 180;
    @Min(1) private int resendCooldownSeconds = 60;
    @Min(1) private int maxAttempts = 5;
    @Min(1) private int dailyPerUser = 5;
    @Min(1) private int dailyPerPhone = 5;
    @Min(1) private int dailyTotal = 20;
    @Min(1) @Max(50) private int monthlyTotal = 50;

    @PostConstruct
    void validateCredentials() {
        if (isPhoneFlowActive() && !enabled) {
            throw new IllegalStateException("Verified-phone rollout requires SMS to be enabled");
        }
        boolean missingCredentials = provider == Provider.SOLAPI
                ? solapiApiKey.isBlank() || solapiApiSecret.isBlank()
                : accessKey.isBlank() || secretKey.isBlank() || serviceId.isBlank();
        if (enabled && (missingCredentials || !senderNumber.matches("[0-9]{8,15}") || codeSecret.length() < 32)) {
            throw new IllegalStateException("Selected SMS provider credentials, registered sender and a 32+ character OTP secret are required");
        }
    }

    public boolean isPhoneFlowActive() {
        return rolloutMode == RolloutMode.VERIFIED_PHONE;
    }
}
