package CamNecT.server.domain.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class ChatPresenceServiceImpl implements ChatPresenceService {

    private final Map<PresenceKey, Set<SubscriptionKey>> subscriptionsByPresence = new HashMap<>();
    private final Map<String, Map<String, PresenceKey>> presencesBySession = new HashMap<>();
    private final Map<SubscriptionKey, UnreadBoundary> subscriptionBoundaries = new HashMap<>();
    // A GET has no socket lifecycle yet. Keep its boundary briefly for the following SUBSCRIBE.
    private final Cache<EntryKey, Long> awaitingSubscription = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30)).maximumSize(10_000).build();

    @Override
    public synchronized void enter(Long roomId, Long userId, String sessionId, String subscriptionId,
                                   String loginSessionId, Long firstUnreadMessageId) {
        if (roomId == null || userId == null
                || !StringUtils.hasText(sessionId) || !StringUtils.hasText(subscriptionId)) {
            log.warn("[chat-presence] skip invalid enter. room={}, user={}, session={}, subscription={}",
                    roomId, userId, sessionId, subscriptionId);
            return;
        }

        PresenceKey presenceKey = new PresenceKey(roomId, userId);
        SubscriptionKey subscriptionKey = new SubscriptionKey(sessionId, subscriptionId);
        Map<String, PresenceKey> sessionPresences = presencesBySession
                .computeIfAbsent(sessionId, ignored -> new HashMap<>());

        PresenceKey previousPresence = sessionPresences.put(subscriptionId, presenceKey);
        if (previousPresence != null && !previousPresence.equals(presenceKey)) {
            removeSubscription(previousPresence, subscriptionKey);
        }

        EntryKey entry = new EntryKey(presenceKey, loginSessionId);
        UnreadBoundary current = subscribedBoundary(entry);
        Long pending = awaitingSubscription.asMap().remove(entry);
        if (current == null) {
            current = new UnreadBoundary(loginSessionId, pending != null ? pending : firstUnreadMessageId);
        }
        subscriptionsByPresence.computeIfAbsent(presenceKey, ignored -> new HashSet<>()).add(subscriptionKey);
        subscriptionBoundaries.put(subscriptionKey, current);
        log.debug("[chat-presence] enter room={}, user={}, session={}, subscription={}",
                roomId, userId, sessionId, subscriptionId);
    }

    @Override
    public synchronized Long firstUnreadForEntry(Long roomId, Long userId, String loginSessionId, Long firstUnreadOnGet) {
        EntryKey entry = new EntryKey(new PresenceKey(roomId, userId), loginSessionId);
        UnreadBoundary current = subscribedBoundary(entry);
        if (current != null) return current.messageId();
        if (firstUnreadOnGet == null) return awaitingSubscription.getIfPresent(entry);
        return awaitingSubscription.get(entry, ignored -> firstUnreadOnGet);
    }

    private UnreadBoundary subscribedBoundary(EntryKey entry) {
        for (SubscriptionKey subscription : subscriptionsByPresence.getOrDefault(entry.presence(), Set.of())) {
            UnreadBoundary boundary = subscriptionBoundaries.get(subscription);
            if (Objects.equals(boundary.loginSessionId(), entry.loginSessionId())) return boundary;
        }
        return null;
    }

    @Override
    public synchronized void leaveSubscription(String sessionId, String subscriptionId) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(subscriptionId)) return;

        Map<String, PresenceKey> sessionPresences = presencesBySession.get(sessionId);
        if (sessionPresences == null) return;

        PresenceKey presenceKey = sessionPresences.remove(subscriptionId);
        if (presenceKey != null) {
            removeSubscription(presenceKey, new SubscriptionKey(sessionId, subscriptionId));
        }
        removeEmptySession(sessionId, sessionPresences);
        log.debug("[chat-presence] unsubscribe session={}, subscription={}", sessionId, subscriptionId);
    }

    @Override
    public synchronized void leaveRoom(Long roomId, Long userId, String sessionId, String loginSessionId) {
        if (roomId == null || userId == null || !StringUtils.hasText(sessionId)) return;

        PresenceKey target = new PresenceKey(roomId, userId);
        awaitingSubscription.invalidate(new EntryKey(target, loginSessionId));
        Map<String, PresenceKey> sessionPresences = presencesBySession.get(sessionId);
        if (sessionPresences == null) return;

        var iterator = sessionPresences.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PresenceKey> entry = iterator.next();
            if (!target.equals(entry.getValue())) continue;

            removeSubscription(target, new SubscriptionKey(sessionId, entry.getKey()));
            iterator.remove();
        }
        removeEmptySession(sessionId, sessionPresences);
        log.debug("[chat-presence] leave-room room={}, user={}, session={}", roomId, userId, sessionId);
    }

    @Override
    public synchronized void leaveSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) return;

        Map<String, PresenceKey> sessionPresences = presencesBySession.remove(sessionId);
        if (sessionPresences == null) return;

        sessionPresences.forEach((subscriptionId, presenceKey) ->
                removeSubscription(presenceKey, new SubscriptionKey(sessionId, subscriptionId)));
        log.debug("[chat-presence] leave-session session={}", sessionId);
    }

    @Override
    public synchronized boolean isPresent(Long roomId, Long userId) {
        Set<SubscriptionKey> subscriptions = subscriptionsByPresence.get(new PresenceKey(roomId, userId));
        boolean present = subscriptions != null && !subscriptions.isEmpty();

        log.debug("[chat-presence] check room={}, user={}, present={}", roomId, userId, present);
        return present;
    }

    private void removeSubscription(PresenceKey presenceKey, SubscriptionKey subscriptionKey) {
        subscriptionBoundaries.remove(subscriptionKey);
        Set<SubscriptionKey> subscriptions = subscriptionsByPresence.get(presenceKey);
        if (subscriptions == null) return;

        subscriptions.remove(subscriptionKey);
        if (subscriptions.isEmpty()) subscriptionsByPresence.remove(presenceKey);
    }

    private void removeEmptySession(String sessionId, Map<String, PresenceKey> sessionPresences) {
        if (sessionPresences.isEmpty()) presencesBySession.remove(sessionId);
    }

    private record PresenceKey(Long roomId, Long userId) {}
    private record EntryKey(PresenceKey presence, String loginSessionId) {}
    private record SubscriptionKey(String sessionId, String subscriptionId) {}
    private record UnreadBoundary(String loginSessionId, Long messageId) {}
}
