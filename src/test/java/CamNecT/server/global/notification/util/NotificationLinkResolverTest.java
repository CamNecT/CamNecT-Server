package CamNecT.server.global.notification.util;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.notification.event.CoffeeChatAcceptedEvent;
import CamNecT.server.global.notification.event.SimpleNotifiableEvent;
import CamNecT.server.global.notification.model.FrontLinkProperties;
import CamNecT.server.global.notification.model.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationLinkResolverTest {

    @Test
    void resolveOrFallbackKeepsExplicitLink() {
        NotificationLinkResolver resolver = new NotificationLinkResolver(properties());
        SimpleNotifiableEvent event = SimpleNotifiableEvent.of(
                1L,
                2L,
                NotificationType.COFFEE_CHAT_REQUESTED,
                "message",
                null,
                null,
                null,
                "/custom-link"
        );

        assertThat(resolver.resolveOrFallback(event)).isEqualTo("/custom-link");
    }

    @Test
    void resolveOrFallbackUsesFallbackWhenRequiredIdIsMissing() {
        NotificationLinkResolver resolver = new NotificationLinkResolver(properties());
        CoffeeChatAcceptedEvent event = new CoffeeChatAcceptedEvent(1L, 2L, null, 10L);

        assertThrows(CustomException.class, () -> resolver.resolve(event));
        assertThat(resolver.resolveOrFallback(event)).isEqualTo("/fallback");
    }

    @Test
    void resolveBuildsChatRoomLink() {
        NotificationLinkResolver resolver = new NotificationLinkResolver(properties());
        CoffeeChatAcceptedEvent event = new CoffeeChatAcceptedEvent(1L, 2L, 99L, 10L);

        assertThat(resolver.resolve(event)).isEqualTo("/chat/99");
    }

    private FrontLinkProperties properties() {
        FrontLinkProperties p = new FrontLinkProperties();
        p.setFallback("/fallback");
        p.setCommunityPost("/community/post/{postId}");
        p.setChatRequest("/chat/requests/{requestId}");
        p.setChatRoom("/chat/{roomId}");
        return p;
    }
}
