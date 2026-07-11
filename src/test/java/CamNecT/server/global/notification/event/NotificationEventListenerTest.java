package CamNecT.server.global.notification.event;

import CamNecT.server.global.notification.model.NotificationType;
import CamNecT.server.global.notification.service.NotificationService;
import CamNecT.server.global.notification.service.NotificationWsPublisher;
import CamNecT.server.global.notification.service.PushDeviceService;
import CamNecT.server.global.notification.util.FCMSender;
import CamNecT.server.global.notification.util.NotificationLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock NotificationService notificationService;
    @Mock FCMSender fcmSender;
    @Mock PushDeviceService pushDeviceService;
    @Mock NotificationWsPublisher notificationWsPublisher;
    @Mock NotificationLinkResolver notificationLinkResolver;

    @InjectMocks NotificationEventListener listener;

    @Test
    void persistSkipsSelfNotification() {
        NotifiableEvent event = event(1L, 1L);

        listener.persist(event);

        verifyNoInteractions(notificationService, notificationLinkResolver);
    }

    @Test
    void persistStoresResolvedLink() {
        NotifiableEvent event = event(2L, 1L);
        when(notificationLinkResolver.resolve(event)).thenReturn("/community/posts/10");

        listener.persist(event);

        verify(notificationService).create(
                2L,
                1L,
                NotificationType.POST_COMMENTED,
                "새 댓글",
                10L,
                20L,
                null,
                "/community/posts/10"
        );
    }

    @Test
    void pushContinuesToFcmWhenWebSocketFails() throws Exception {
        NotifiableEvent event = event(2L, 1L);
        when(notificationLinkResolver.resolve(event)).thenReturn("/community/posts/10");
        doThrow(new IllegalStateException("ws down"))
                .when(notificationWsPublisher).sendToUser(eq(2L), any());
        when(pushDeviceService.findEnabledTokens(2L)).thenReturn(List.of("token"));
        when(fcmSender.sendToTokens(eq(List.of("token")), anyMap()))
                .thenReturn(new FCMSender.SendResult(1, 1, 0, List.of()));

        assertDoesNotThrow(() -> listener.push(event));

        verify(fcmSender).sendToTokens(eq(List.of("token")), anyMap());
        verify(pushDeviceService).disableTokens(List.of());
    }

    @Test
    void pushDoesNotPropagateTokenLookupFailure() {
        NotifiableEvent event = event(2L, 1L);
        when(notificationLinkResolver.resolve(event)).thenReturn("/community/posts/10");
        when(pushDeviceService.findEnabledTokens(2L))
                .thenThrow(new IllegalStateException("database down"));

        assertDoesNotThrow(() -> listener.push(event));

        verifyNoInteractions(fcmSender);
    }

    private static NotifiableEvent event(Long receiverId, Long actorId) {
        return SimpleNotifiableEvent.of(
                receiverId,
                actorId,
                NotificationType.POST_COMMENTED,
                "새 댓글",
                10L,
                20L
        );
    }
}
