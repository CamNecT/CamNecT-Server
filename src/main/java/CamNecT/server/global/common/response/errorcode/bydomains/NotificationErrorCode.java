package CamNecT.server.global.common.response.errorcode.bydomains;

import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    // xx000은 전역 HTTP 오류 예약, 454xx는 Notification 리소스 오류
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 45401, "해당 알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
