package CamNecT.server.domain.verification.phone;

import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PhoneErrorCode implements BaseErrorCode {
    INVALID_PHONE(HttpStatus.BAD_REQUEST, 42040, "올바른 국내 휴대전화 번호를 입력해 주세요."),
    PHONE_REQUIRED(HttpStatus.BAD_REQUEST, 42041, "전화번호를 먼저 등록해 주세요."),
    INVALID_CHALLENGE(HttpStatus.BAD_REQUEST, 42042, "인증 요청이 만료되었거나 유효하지 않습니다."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, 42043, "인증번호가 올바르지 않습니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, 42940, "이미 인증된 전화번호입니다."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, 42941, "인증 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, 42942, "인증번호 입력 횟수를 초과했습니다."),
    SMS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 42540, "현재 문자 인증을 사용할 수 없습니다."),
    SMS_SEND_FAILED(HttpStatus.BAD_GATEWAY, 42541, "문자 발송을 확인하지 못했습니다. 잠시 후 다시 요청해 주세요.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
