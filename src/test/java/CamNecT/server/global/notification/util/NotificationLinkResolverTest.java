package CamNecT.server.global.notification.util;

import CamNecT.server.global.notification.event.CoffeeChatAcceptedEvent;
import CamNecT.server.global.notification.event.SimpleNotifiableEvent;
import CamNecT.server.global.notification.model.FrontLinkProperties;
import CamNecT.server.global.notification.model.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationLinkResolverTest {

    @Test
    void resolveOrFallbackKeepsExplicitLink() {
        NotificationLinkResolver resolver =
                new NotificationLinkResolver(properties());

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

        assertThat(resolver.resolveOrFallback(event))
                .isEqualTo("/custom-link");
    }

    @Test
    void resolveAndResolveOrFallbackUseFallbackWhenRequiredIdIsMissing() {
        NotificationLinkResolver resolver =
                new NotificationLinkResolver(properties());

        CoffeeChatAcceptedEvent event =
                new CoffeeChatAcceptedEvent(
                        1L,
                        2L,
                        null,
                        10L
                );

        assertThat(resolver.resolve(event))
                .isEqualTo("/fallback");

        assertThat(resolver.resolveOrFallback(event))
                .isEqualTo("/fallback");
    }

    @Test
    void resolveBuildsChatRoomLink() {
        NotificationLinkResolver resolver =
                new NotificationLinkResolver(properties());

        CoffeeChatAcceptedEvent event =
                new CoffeeChatAcceptedEvent(
                        1L,
                        2L,
                        99L,
                        10L
                );

        assertThat(resolver.resolve(event))
                .isEqualTo("/chat/99");
    }

    @Test
    void malformedTemplateFallsBackInsteadOfReturningUnresolvedLink() {
        FrontLinkProperties properties = properties();
        properties.setChatRoom("/chat/room");
        NotificationLinkResolver resolver = new NotificationLinkResolver(properties);

        CoffeeChatAcceptedEvent event = new CoffeeChatAcceptedEvent(1L, 2L, 99L, 10L);

        assertThat(resolver.resolve(event)).isEqualTo("/fallback");
    }

    private FrontLinkProperties properties() {
        FrontLinkProperties properties = new FrontLinkProperties();

        properties.setFallback("/fallback");
        properties.setCommunityPost("/community/post/{postId}");
        properties.setChatRequest("/chat/requests/{requestId}");
        properties.setChatRoom("/chat/{roomId}");

        return properties;
    }
}
