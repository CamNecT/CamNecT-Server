package CamNecT.server.global.notification.util;

import CamNecT.server.global.notification.event.NotifiableEvent;
import CamNecT.server.global.notification.model.FrontLinkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationLinkResolver {

    private final FrontLinkProperties p;

    public String resolveOrFallback(NotifiableEvent e) {
        try {
            return resolve(e);
        } catch (Exception ex) {
            return fallback();
        }
    }

    public String resolve(NotifiableEvent e) {
        // 이벤트가 직접 링크를 주면 우선
        if (hasText(e.link())) return e.link();

        try {
            return switch (e.type()) {

                // 커뮤니티: postId 필수
                case POST_COMMENTED, COMMENT_ACCEPTED, COMMENT_REPLIED, FOLLOWING_POSTED ->
                        template(p.getCommunityPost(), "postId", requirePostId(e));

                // 요청 상세: requestId 필수 (커피챗/팀원모집 공용)
                case COFFEE_CHAT_REQUESTED, TEAM_APPLICATION_RECEIVED ->
                        template(p.getChatRequest(), "requestId", requireRequestId(e));

                // 채팅방: roomId 필수 (승인/채팅수신 공용)
                case COFFEE_CHAT_ACCEPTED, TEAM_RECRUIT_ACCEPTED, CHAT_MESSAGE_RECEIVED ->
                        template(p.getChatRoom(), "roomId", requireRoomId(e));

                // 포인트 등: 별도 화면이 없으면 fallback
                case POINT_EARNED, POINT_SPENT, ADMIN_ANNOUNCEMENT -> fallback();
            };
        } catch (Exception ex) {
            log.warn("[notification-link] fallback applied. type={}", e.type(), ex);
            return fallback();
        }
    }

    private String fallback() {
        return hasText(p.getFallback()) ? p.getFallback() : "/";
    }

    private String template(String tpl, String key, Long value) {
        if (!hasText(tpl) || value == null) return fallback();
        return tpl.replace("{" + key + "}", String.valueOf(value));
    }

    private Long requirePostId(NotifiableEvent e) {
        Long v = e.postId();
        if (v == null) throw new IllegalStateException("postId is required for " + e.type());
        return v;
    }

    private Long requireRequestId(NotifiableEvent e) {
        Long v = e.requestId();
        if (v == null) throw new IllegalStateException("requestId is required for " + e.type());
        return v;
    }

    private Long requireRoomId(NotifiableEvent e) {
        Long v = e.roomId();
        if (v == null) throw new IllegalStateException("roomId is required for " + e.type());
        return v;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
