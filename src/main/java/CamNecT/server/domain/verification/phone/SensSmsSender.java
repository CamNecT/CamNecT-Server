package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.phone-verification", name = "provider", havingValue = "sens")
public class SensSmsSender implements SmsSender {
    private final PhoneVerificationProperties properties;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final HttpClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public SensSmsSender(PhoneVerificationProperties properties, ObjectMapper mapper, Clock clock) {
        this(properties, mapper, clock, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    SensSmsSender(PhoneVerificationProperties properties, ObjectMapper mapper, Clock clock, HttpClient client) {
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
        this.client = client;
    }

    @Override
    public String send(String phone, String code) {
        if (!properties.isEnabled()) throw new CustomException(PhoneErrorCode.SMS_UNAVAILABLE);
        try {
            String path = "/sms/v2/services/" + properties.getServiceId() + "/messages";
            String timestamp = Long.toString(clock.millis());
            String body = mapper.writeValueAsString(Map.of(
                    "type", "SMS", "contentType", "COMM", "countryCode", "82",
                    "from", properties.getSenderNumber(),
                    "content", "[CamNecT] 인증번호 [" + code + "]를 입력해 주세요.",
                    "messages", List.of(Map.of("to", phone))));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://sens.apigw.ntruss.com" + path))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", properties.getAccessKey())
                    .header("x-ncp-apigw-signature-v2", signature(path, timestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 202) throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
            var json = mapper.readTree(response.body());
            String requestId = json.path("requestId").asText("");
            if (!"202".equals(json.path("statusCode").asText()) || requestId.isBlank() || requestId.length() > 100) {
                throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
            }
            return requestId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
        } catch (Exception e) {
            // Never retain/log provider bodies, phone numbers, OTPs or credentials in an exception.
            throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
        }
    }

    String signature(String path, String timestamp) throws java.security.GeneralSecurityException {
        String signing = "POST " + path + "\n" + timestamp + "\n" + properties.getAccessKey();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(signing.getBytes(StandardCharsets.UTF_8)));
    }
}
