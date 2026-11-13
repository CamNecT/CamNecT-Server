package CamNecT.server.domain.chat.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPresenceServiceImplTest {

    private final ChatPresenceServiceImpl presenceService = new ChatPresenceServiceImpl();

    @Test
    void onlyTheMatchingRoomUserAndLoginCanConsumeASubscriptionBoundary() {
        presenceService.enter(10L, 1L, "socket", "sub", "login-a", 12L);
        presenceService.enter(10L, 1L, "socket", "sub", "login-a", null);
        assertThat(presenceService.takeFirstUnreadMessageId(10L, 2L, "login-a")).isNull();
        assertThat(presenceService.takeFirstUnreadMessageId(20L, 1L, "login-a")).isNull();
        assertThat(presenceService.takeFirstUnreadMessageId(10L, 1L, "login-b")).isNull();
        assertThat(presenceService.takeFirstUnreadMessageId(10L, 1L, "login-a")).isEqualTo(12L);
        assertThat(presenceService.takeFirstUnreadMessageId(10L, 1L, "login-a")).isNull();
    }

    @Test
    void disconnectDiscardsTheBoundaryBeforeReentry() {
        presenceService.enter(10L, 1L, "socket", "sub", "login", 12L);
        presenceService.leaveSession("socket");
        presenceService.enter(10L, 1L, "socket", "sub", "login", null);
        assertThat(presenceService.takeFirstUnreadMessageId(10L, 1L, "login")).isNull();
    }

    @Test
    void disconnectingOneOfMultipleSessionsKeepsUserPresent() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a", "login", null);
        presenceService.enter(10L, 1L, "session-b", "subscription-b", "login", null);

        presenceService.leaveSession("session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isTrue();

        presenceService.leaveSession("session-b");
        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }

    @Test
    void unsubscribeRemovesOnlyThatSubscription() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a", "login", null);
        presenceService.enter(20L, 1L, "session-a", "subscription-b", "login", null);

        presenceService.leaveSubscription("session-a", "subscription-a");

        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
        assertThat(presenceService.isPresent(20L, 1L)).isTrue();
    }

    @Test
    void explicitLeaveRemovesOnlyCurrentSessionsRoomPresence() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a", "login", null);
        presenceService.enter(10L, 1L, "session-b", "subscription-b", "login", null);

        presenceService.leaveRoom(10L, 1L, "session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isTrue();
        presenceService.leaveRoom(10L, 1L, "session-b");
        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }

    @Test
    void duplicateSubscribeAndCleanupAreIdempotent() {
        presenceService.enter(10L, 1L, "session-a", "subscription-a", "login", null);
        presenceService.enter(10L, 1L, "session-a", "subscription-a", "login", null);

        presenceService.leaveSubscription("session-a", "subscription-a");
        presenceService.leaveSubscription("session-a", "subscription-a");
        presenceService.leaveSession("session-a");

        assertThat(presenceService.isPresent(10L, 1L)).isFalse();
    }
}
