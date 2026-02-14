package CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains;

import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    // 450xx - 요청/검증
    REQUIRED_ID_MISSING(HttpStatus.BAD_REQUEST, 45001, "알림 링크 생성에 필요한 식별자가 누락되었습니다."),
    REQUIRED_ROOM_ID_MISSING(HttpStatus.BAD_REQUEST, 45002, "알림 링크 생성에 필요한 채팅방 ID가 누락되었습니다."),

    // 55xxx - 서버에러
    INVALID_LINK_TEMPLATE(HttpStatus.INTERNAL_SERVER_ERROR, 55001, "알림 링크 템플릿 설정이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
