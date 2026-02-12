package CamNecT.CamNecT_Server.global.notification.model;

public enum NotificationType {

    //포인트 관련
    POINT_EARNED,
    POINT_SPENT,

    //COMMUNITY 관련
    POST_COMMENTED,
    COMMENT_ACCEPTED,
    COMMENT_REPLIED,

    //COFFEE_CHAT로직관련(전송)
    COFFEE_CHAT_REQUESTED,
    TEAM_APPLICATION_RECEIVED,

    //COFFEE_CHAT로직관련(승인)
    COFFEE_CHAT_ACCEPTED,
    TEAM_RECRUIT_ACCEPTED,

    //COFFEE_CHAT 채팅 메세지 수신
    CHAT_MESSAGE_RECEIVED,

    TEAM_RECRUIT_CLOSED
}
