package CamNecT.CamNecT_Server.global.notification.event;

import CamNecT.CamNecT_Server.global.notification.model.NotificationType;

public record TeamRecruitAcceptedEvent(
        Long receiverUserId,
        Long actorUserId,
        Long roomId,
        Long recruitId
) implements NotifiableEvent {
    @Override public NotificationType type() { return NotificationType.TEAM_RECRUIT_ACCEPTED; }
    @Override public String message() { return "팀원 모집 신청이 승인되었습니다."; }
    @Override public Long roomId() { return roomId; }
    @Override public Long requestId() { return recruitId; } // 필요하면 부가 데이터로
}