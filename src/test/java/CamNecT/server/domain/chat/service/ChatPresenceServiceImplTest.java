package CamNecT.server.domain.chat.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPresenceServiceImplTest {

    private final ChatPresenceServiceImpl presenceService = new ChatPresenceServiceImpl();

    @Test
    void onlyTheMatchingRoomUserAndLoginSeeTheBoundaryOnEveryRefetch() {
        presenceService.enter(10L, 1L, "socket", "sub", "login-a", 12L);
        presenceService.enter(10L, 1L, "socket", "sub", "login-a", null);
        assertThat(presenceService.firstUnreadForEntry(10L, 2L, "login-a", null)).isNull();
        assertThat(presenceService.firstUnreadForEntry(20L, 1L, "login-a", null)).isNull();
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login-b", null)).isNull();
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login-a", null)).isEqualTo(12L);
        presenceService.enter(10L, 1L, "second-socket", "sub", "login-a", null);
        presenceService.leaveSubscription("socket", "sub");
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login-a", null)).isEqualTo(12L);
    }

    @Test
    void repeatedGetsBeforeSubscribeKeepTheOldestBoundaryAndExplicitLeaveEndsTheVisit() {
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login", 1204L)).isEqualTo(1204L);
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login", 1206L)).isEqualTo(1204L);
        presenceService.leaveRoom(10L, 1L, "socket", "login");
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login", null)).isNull();
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login", 1207L)).isEqualTo(1207L);
    }

    @Test
    void disconnectDiscardsTheBoundaryBeforeReentry() {
        presenceService.enter(10L, 1L, "socket", "sub", "login", 12L);
        presenceService.leaveSession("socket");
        presenceService.enter(10L, 1L, "socket", "sub", "login", null);
        assertThat(presenceService.firstUnreadForEntry(10L, 1L, "login", null)).isNull();
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

        presenceService.leaveRoom(10L, 1L, "session-a", "login");

        assertThat(presenceService.isPresent(10L, 1L)).isTrue();
        presenceService.leaveRoom(10L, 1L, "session-b", "login");
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
