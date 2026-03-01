package CamNecT.server.global.notification.util;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class FCMSender {
    @Value("${app.push.enabled:true}")
    private boolean pushEnabled;

    private static final int CHUNK_SIZE = 500; //500토큰 제한

    public SendResult sendToTokens(List<String> tokens, Map<String, String> data)
            throws FirebaseMessagingException {

        if (!pushEnabled) {
            return SendResult.empty();
        }

        if (tokens == null || tokens.isEmpty()) {
            return SendResult.empty();
        }

        int requested = tokens.size();
        int success = 0;
        int failure = 0;
        List<String> invalidTokens = new ArrayList<>();

        for (int start = 0; start < tokens.size(); start += CHUNK_SIZE) {
            List<String> chunk = tokens.subList(start, Math.min(tokens.size(), start + CHUNK_SIZE));

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    //중복출력방지를 위해 notification 부분주석처리
//                    .setNotification(Notification.builder()
//                            .setTitle(title)
//                            .setBody(body)
//                            .build())
                    .putAllData(data == null ? Map.of() : data)
                    .build();

            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            success += resp.getSuccessCount();
            failure += resp.getFailureCount();

            List<SendResponse> responses = resp.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (r.isSuccessful()) continue;

                Exception ex = r.getException();
                if (ex instanceof FirebaseMessagingException fme) {
                    MessagingErrorCode code = fme.getMessagingErrorCode();
                    // 대표적인 "토큰 무효" 케이스들
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalidTokens.add(chunk.get(i));
                    }
                }
            }
        }

        return new SendResult(requested, success, failure, invalidTokens);
    }

    public record SendResult(int requested, int success, int failure, List<String> invalidTokens) {
        public static SendResult empty() {
            return new SendResult(0, 0, 0, List.of());
        }
    }



//    LEGACY: notification 포함 (혹시 몰라서 유지)
//    public SendResult sendToTokens(List<String> tokens, String title, String body, Map<String, String> data)
//            throws FirebaseMessagingException {
//
//        if (tokens == null || tokens.isEmpty()) return SendResult.empty();
//
//        int requested = tokens.size();
//        int success = 0;
//        int failure = 0;
//        List<String> invalidTokens = new ArrayList<>();
//
//        for (int start = 0; start < tokens.size(); start += CHUNK_SIZE) {
//            List<String> chunk = tokens.subList(start, Math.min(tokens.size(), start + CHUNK_SIZE));
//
//            MulticastMessage message = MulticastMessage.builder()
//                    .addAllTokens(chunk)
//                    .setNotification(Notification.builder()
//                            .setTitle(title)
//                            .setBody(body)
//                            .build())
//                    .putAllData(data == null ? Map.of() : data)
//                    .build();
//
//            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);
//            success += resp.getSuccessCount();
//            failure += resp.getFailureCount();
//
//            List<SendResponse> responses = resp.getResponses();
//            for (int i = 0; i < responses.size(); i++) {
//                SendResponse r = responses.get(i);
//                if (r.isSuccessful()) continue;
//
//                Exception ex = r.getException();
//                if (ex instanceof FirebaseMessagingException fme) {
//                    MessagingErrorCode code = fme.getMessagingErrorCode();
//                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
//                        invalidTokens.add(chunk.get(i));
//                    }
//                }
//            }
//        }
//        return new SendResult(requested, success, failure, invalidTokens);
//    }
}
