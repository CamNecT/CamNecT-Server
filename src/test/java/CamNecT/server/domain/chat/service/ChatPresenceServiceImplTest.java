package CamNecT.server.domain.chat.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPresenceServiceImplTest {

    private final ChatPresenceServiceImpl presenceService = new ChatPresenceServiceImpl();

    @Test
    void disconnectingOneOfMultipleSessionsKeepsUserPresent() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a");
        presenceService.enter(10L, 1L, "session-b", "subscription-b");

        presenceService.leaveSession("session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isTrue();

        presenceService.leaveSession("session-b");
        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }

    @Test
    void unsubscribeRemovesOnlyThatSubscription() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a");
        presenceService.enter(20L, 1L, "session-a", "subscription-b");

        presenceService.leaveSubscription("session-a", "subscription-a");

        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
        assertThat(presenceService.isPresent(20L, 1L)).isTrue();
    }

    @Test
    void explicitLeaveRemovesOnlyCurrentSessionsRoomPresence() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a");
        presenceService.enter(10L, 1L, "session-b", "subscription-b");

        presenceService.leaveRoom(10L, 1L, "session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isTrue();
        presenceService.leaveRoom(10L, 1L, "session-b");
        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }

    @Test
    void duplicateSubscribeAndCleanupAreIdempotent() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a");
        presenceService.enter(10L, 1L, "session-a", "subscription-a");

        presenceService.leaveSubscription("session-a", "subscription-a");
        presenceService.leaveSubscription("session-a", "subscription-a");
        presenceService.leaveSession("session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }
}
