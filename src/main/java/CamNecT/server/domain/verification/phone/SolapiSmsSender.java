package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(prefix = "app.phone-verification", name = "provider", havingValue = "solapi", matchIfMissing = true)
public class SolapiSmsSender implements SmsSender {
    private final PhoneVerificationProperties properties;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final HttpClient client;
    private final Supplier<String> salts;

    @Autowired
    public SolapiSmsSender(PhoneVerificationProperties properties, ObjectMapper mapper, Clock clock) {
        this(properties, mapper, clock, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build(),
                () -> UUID.randomUUID().toString().replace("-", ""));
    }

    SolapiSmsSender(PhoneVerificationProperties properties, ObjectMapper mapper, Clock clock,
                    HttpClient client, Supplier<String> salts) {
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
        this.client = client;
        this.salts = salts;
    }

    @Override
    public String send(String phone, String code) {
        if (!properties.isEnabled()) throw new CustomException(PhoneErrorCode.SMS_UNAVAILABLE);
        try {
            String date = clock.instant().toString();
            String salt = salts.get();
            String text = "[CamNecT] 인증번호 [" + code + "]를 입력해 주세요.";
            String body = mapper.writeValueAsString(Map.of(
                    "messages", List.of(Map.of("to", phone, "from", properties.getSenderNumber(),
                            "text", text, "type", "SMS", "country", "82", "autoTypeDetect", false)),
                    "strict", true, "allowDuplicates", false));
            String authorization = "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                    .formatted(properties.getSolapiApiKey(), date, salt, signature(date, salt));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.solapi.com/messages/v4/send-many/detail"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
            var json = mapper.readTree(response.body());
            var failed = json.path("failedMessageList");
            var group = json.path("groupInfo");
            var count = group.path("count");
            String groupId = group.path("groupId").asText("");
            String status = group.path("status").asText("");
            // HTTP 200 can still contain rejected messages. Confirm this one SMS was accepted.
            if (!failed.isArray() || !failed.isEmpty() || groupId.isBlank() || groupId.length() > 100
                    || !("SENDING".equals(status) || "COMPLETE".equals(status))
                    || count.path("total").asInt(-1) != 1
                    || count.path("registeredSuccess").asInt(-1) != 1
                    || count.path("registeredFailed").asInt(-1) != 0
                    || count.path("sentFailed").asInt(-1) != 0
                    || ("COMPLETE".equals(status) && count.path("sentSuccess").asInt(-1) != 1)) {
                throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
            }
            return groupId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
        } catch (Exception e) {
            // Never expose provider bodies, phone numbers, codes or credentials; do not retry.
            throw new CustomException(PhoneErrorCode.SMS_SEND_FAILED);
        }
    }

    String signature(String date, String salt) throws java.security.GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getSolapiApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
    }
}
