package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.net.http.*;
import java.nio.charset.Charset;
import java.time.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SensSmsSenderTest {
    private final PhoneVerificationProperties properties = configured();
    private final HttpClient client = mock(HttpClient.class);
    private final SensSmsSender sender = new SensSmsSender(properties, new ObjectMapper(),
            Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC), client);

    @Test
    void createsSignedKoreanTransactionalSmsAndRequiresAcceptedResponse() throws Exception {
        HttpResponse<String> response = response(202, "{\"statusCode\":\"202\",\"requestId\":\"request-123\"}");
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        assertThat(sender.send("01012345678", "123456")).isEqualTo("request-123");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(request.getValue().uri().toString())
                .isEqualTo("https://sens.apigw.ntruss.com/sms/v2/services/ncp:sms:kr:test:camnect/messages");
        assertThat(request.getValue().method()).isEqualTo("POST");
        assertThat(request.getValue().headers().firstValue("x-ncp-apigw-timestamp")).contains("1700000000000");
        assertThat(request.getValue().headers().firstValue("x-ncp-apigw-signature-v2"))
                .contains("P0rl9cHtqZ9cAHSO7STEsc0uqCpp9tROTj+IXmNEN4I=");
        assertThat(request.getValue().timeout()).contains(Duration.ofSeconds(5));

        String body = readBody(request.getValue());
        var json = new ObjectMapper().readTree(body);
        assertThat(json.path("type").asText()).isEqualTo("SMS");
        assertThat(json.path("contentType").asText()).isEqualTo("COMM");
        assertThat(json.path("countryCode").asText()).isEqualTo("82");
        assertThat(json.path("from").asText()).isEqualTo("01000000000");
        assertThat(json.path("messages").get(0).path("to").asText()).isEqualTo("01012345678");
        assertThat(json.path("content").asText()).contains("123456");
        assertThat(json.path("content").asText().getBytes(Charset.forName("EUC-KR")).length).isLessThanOrEqualTo(90);
    }

    @Test
    void rejectsNonAcceptedOrMalformedProviderResponsesWithoutLeakingBody() throws Exception {
        for (HttpResponse<String> response : java.util.List.of(
                response(403, "secret phone 01012345678 code 123456"),
                response(200, "{\"statusCode\":\"202\",\"requestId\":\"request-123\"}"),
                response(202, "{\"statusCode\":\"202\"}"),
                response(202, "invalid-json"),
                response(202, "{\"statusCode\":\"400\",\"requestId\":\"request-123\"}"))) {
            when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
            assertThatThrownBy(() -> sender.send("01012345678", "123456"))
                    .isInstanceOfSatisfying(CustomException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(PhoneErrorCode.SMS_SEND_FAILED);
                        assertThat(e.getMessage()).doesNotContain("01012345678", "123456", "secret");
                        assertThat(e.getCause()).isNull();
                    });
        }
    }

    @Test
    void timeoutIsNotRetried() throws Exception {
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new HttpTimeoutException("timeout"));
        assertThatThrownBy(() -> sender.send("01012345678", "123456")).isInstanceOf(CustomException.class);
        verify(client, times(1)).send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void disabledProviderNeverCallsNetwork() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> sender.send("01012345678", "123456")).isInstanceOf(CustomException.class);
        verifyNoInteractions(client);
    }

    @Test
    void productionConfigurationRequiresCredentialsAndIndependentCodeSecret() {
        var missing = new PhoneVerificationProperties();
        missing.setEnabled(true);
        assertThatThrownBy(missing::validateCredentials).isInstanceOf(IllegalStateException.class);
        properties.setCodeSecret("short");
        assertThatThrownBy(properties::validateCredentials).isInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private String readBody(HttpRequest request) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        var done = new java.util.concurrent.CompletableFuture<Void>();
        request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { done.completeExceptionally(error); }
            public void onComplete() { done.complete(null); }
        });
        done.get(2, java.util.concurrent.TimeUnit.SECONDS);
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private PhoneVerificationProperties configured() {
        var p = new PhoneVerificationProperties();
        p.setProvider(PhoneVerificationProperties.Provider.SENS);
        p.setEnabled(true);
        p.setAccessKey("test-access");
        p.setSecretKey("test-secret");
        p.setServiceId("ncp:sms:kr:test:camnect");
        p.setSenderNumber("01000000000");
        p.setCodeSecret("test-code-secret-with-at-least-32-characters");
        return p;
    }
}
