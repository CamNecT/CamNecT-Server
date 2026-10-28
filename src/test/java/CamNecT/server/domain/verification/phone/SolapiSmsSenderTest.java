package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.net.http.*;
import java.nio.charset.Charset;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolapiSmsSenderTest {
    private static final String ACCEPTED = """
        {"failedMessageList":[],"groupInfo":{"groupId":"G-test","status":"SENDING",
        "count":{"total":1,"registeredSuccess":1,"registeredFailed":0,"sentFailed":0,"sentSuccess":0}}}
        """;
    private final PhoneVerificationProperties properties = configured();
    private final HttpClient client = mock(HttpClient.class);
    private final SolapiSmsSender sender = new SolapiSmsSender(properties, new ObjectMapper(),
            Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC), client,
            () -> "0123456789abcdef0123456789abcdef");

    @Test
    void signsSingleSmsAndDisablesAutomaticUpgradeToLms() throws Exception {
        doReturn(response(200, ACCEPTED)).when(client)
                .send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(sender.send("01012345678", "123456")).isEqualTo("G-test");
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = request.getValue();
        assertThat(sent.uri().toString()).isEqualTo("https://api.solapi.com/messages/v4/send-many/detail");
        assertThat(sent.timeout()).contains(Duration.ofSeconds(5));
        assertThat(sent.headers().firstValue("Authorization")).contains(
                "HMAC-SHA256 apiKey=test-key, date=2023-11-14T22:13:20Z, salt=0123456789abcdef0123456789abcdef, "
                        + "signature=18f603b1871d67daa30480fa478ada4677b37854f43b0d8a45521e2760cf7cd5");
        var body = new ObjectMapper().readTree(readBody(sent));
        assertThat(body.path("messages").size()).isEqualTo(1);
        var message = body.path("messages").get(0);
        assertThat(message.path("to").asText()).isEqualTo("01012345678");
        assertThat(message.path("from").asText()).isEqualTo("01000000000");
        assertThat(message.path("type").asText()).isEqualTo("SMS");
        assertThat(message.path("country").asText()).isEqualTo("82");
        assertThat(message.path("autoTypeDetect").asBoolean()).isFalse();
        assertThat(message.path("text").asText()).contains("123456");
        assertThat(message.path("text").asText().getBytes(Charset.forName("EUC-KR")).length).isLessThanOrEqualTo(90);
        assertThat(message.has("subject")).isFalse();
        assertThat(body.path("strict").asBoolean()).isTrue();
        assertThat(body.path("allowDuplicates").asBoolean()).isFalse();
    }

    @Test
    void http200DoesNotHideRejectedOrUnsentMessages() throws Exception {
        for (String invalid : List.of("{}", "invalid-json",
                ACCEPTED.replace("\"failedMessageList\":[]", "\"failedMessageList\":[{\"reason\":\"secret 123456\"}]"),
                ACCEPTED.replace("SENDING", "PENDING"), ACCEPTED.replace("SENDING", "FAILED"),
                ACCEPTED.replace("SENDING", "COMPLETE"),
                ACCEPTED.replace("\"registeredSuccess\":1", "\"registeredSuccess\":0"),
                ACCEPTED.replace("\"registeredFailed\":0", "\"registeredFailed\":1"),
                ACCEPTED.replace("\"sentFailed\":0", "\"sentFailed\":1"),
                ACCEPTED.replace("\"total\":1", "\"total\":2"),
                ACCEPTED.replace("G-test", ""))) {
            doReturn(response(200, invalid)).when(client)
                    .send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
            assertThatThrownBy(() -> sender.send("01012345678", "123456"))
                    .isInstanceOfSatisfying(CustomException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(PhoneErrorCode.SMS_SEND_FAILED);
                        assertThat(e.getMessage()).doesNotContain("01012345678", "123456", "secret");
                        assertThat(e.getCause()).isNull();
                    });
        }
    }

    @Test
    void acceptsAlreadyCompletedSuccessfulSms() throws Exception {
        doReturn(response(200, ACCEPTED.replace("SENDING", "COMPLETE").replace("\"sentSuccess\":0", "\"sentSuccess\":1")))
                .when(client).send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(sender.send("01012345678", "123456")).isEqualTo("G-test");
    }

    @Test
    void rejectsHttpErrorsAndNeverRetriesTimeouts() throws Exception {
        doReturn(response(403, "secret 123456")).when(client)
                .send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThatThrownBy(() -> sender.send("01012345678", "123456")).isInstanceOf(CustomException.class);
        clearInvocations(client);
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new HttpTimeoutException("secret"));
        assertThatThrownBy(() -> sender.send("01012345678", "123456")).isInstanceOf(CustomException.class);
        verify(client, times(1)).send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void disabledProviderDoesNotUseNetwork() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> sender.send("01012345678", "123456"))
                .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(PhoneErrorCode.SMS_UNAVAILABLE));
        verifyNoInteractions(client);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> result = mock(HttpResponse.class);
        when(result.statusCode()).thenReturn(status);
        when(result.body()).thenReturn(body);
        return result;
    }

    private String readBody(HttpRequest request) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        var done = new java.util.concurrent.CompletableFuture<Void>();
        request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { done.completeExceptionally(error); }
            public void onComplete() { done.complete(null); }
        });
        done.get(2, java.util.concurrent.TimeUnit.SECONDS);
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private PhoneVerificationProperties configured() {
        var p = new PhoneVerificationProperties();
        p.setEnabled(true);
        p.setSolapiApiKey("test-key");
        p.setSolapiApiSecret("test-solapi-secret");
        p.setSenderNumber("01000000000");
        p.setCodeSecret("test-code-secret-with-at-least-32-characters");
        return p;
    }
}
