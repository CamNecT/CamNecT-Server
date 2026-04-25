package CamNecT.server.global.notification.util;

import CamNecT.server.global.notification.model.NotificationType;

public final class NotificationUtil {

    private NotificationUtil() {
    }

    public static String titleOf(NotificationType type) {
        if (type == null) return "[알림]";

        return switch (type) {
            case POINT_EARNED -> "[포인트 적립]";
            case POINT_SPENT -> "[포인트 사용]";
            case POST_COMMENTED -> "[댓글]";
            case COMMENT_ACCEPTED -> "[댓글 채택]";
            case COMMENT_REPLIED -> "[답글]";
            case FOLLOWING_POSTED -> "[새 게시글]";
            case COFFEE_CHAT_REQUESTED -> "[커피챗 요청]";
            case TEAM_APPLICATION_RECEIVED -> "[팀 지원 신청]"; 
            case COFFEE_CHAT_ACCEPTED -> "[커피챗 수락]"; 
            case TEAM_RECRUIT_ACCEPTED -> "[팀 모집 수락]";
            case CHAT_MESSAGE_RECEIVED -> "[새 메시지]";
            case ADMIN_ANNOUNCEMENT ->  "[이벤트]";
            default -> "[알림]";
        };
    }
}