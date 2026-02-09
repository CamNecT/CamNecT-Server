package CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains;

import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CoffeeChatErrorCode implements BaseErrorCode {

    // 480xx - 잘못된 요청 (Bad Request)
    SELF_REQUEST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, 48001, "자기 자신에게는 요청을 보낼 수 없습니다."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, 48002, "이미 수락하거나 거절된 요청입니다."),
    INVALID_CHAT_CONTENT(HttpStatus.BAD_REQUEST, 48003, "메시지 내용은 비어있을 수 없습니다."),
    RECEIVER_COFFEECHAT_DISABLED(HttpStatus.BAD_REQUEST, 48004, "상대방이 커피챗 요청을 받지 않는 상태입니다."),
    COFFEE_CHAT_CLOSED(HttpStatus.BAD_REQUEST, 48005, "이미 닫힌 채팅방입니다."),

    // 483xx - 권한 없음 (Forbidden)
    REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, 48301, "본인의 요청만 처리(수락/거절)할 수 있습니다."),
    CHATROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, 48302, "해당 채팅방에 접근할 권한이 없습니다."),

    // 484xx - 리소스 없음 (Not Found)
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, 48401, "해당 커피챗 요청을 찾을 수 없습니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 48402, "채팅방을 찾을 수 없습니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, 48403, "존재하지 않는 태그가 포함되어 있습니다."),
    REQUESTER_NOT_FOUND(HttpStatus.NOT_FOUND, 48404, "요청자 정보를 찾을 수 없습니다."),
    RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, 48405, "수신자 정보를 찾을 수 없습니다."),

    // 489xx - 충돌/중복 (Conflict)
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, 48901, "이미 대기 중인 커피챗 요청이 존재합니다."),
    CHATROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, 48902, "이미 활성화된 채팅방이 존재합니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
