package CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains;

import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 410xx - 입력/검증
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, 41010, "비밀번호 형식이 올바르지 않습니다."),
    TERMS_REQUIRED(HttpStatus.BAD_REQUEST, 41020, "필수 약관에 동의해야 합니다."),

    // 411xx - 인증/토큰
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 41101, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.UNAUTHORIZED, 41102, "Authorization 헤더 형식이 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 41103, "유효하지 않은 토큰입니다."),
    ACCESS_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, 41104, "Access Token이 필요합니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, 41105, "로그인이 필요합니다."),
    TOKEN_TYPE_NOT_ALLOWED(HttpStatus.UNAUTHORIZED,41106,"토큰타입이 다릅니다."),
    // 413xx - 권한/상태
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, 41301, "이메일 인증이 필요합니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, 41302, "정지된 사용자입니다."),
    USER_ALREADY_EMAIL_VERIFIED(HttpStatus.FORBIDDEN,41303,"이미 인증되었습니다. 로그인으로 넘어가주세요"),

    // 414xx - 리소스
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 41401, "유저를 찾을 수 없습니다."),

    // 419xx - 충돌
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, 41901, "이미 가입된 이메일입니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, 41902, "이미 사용 중인 아이디입니다."),
    PHONENUM_ALREADY_EXISTS(HttpStatus.CONFLICT, 41903, "이미 사용 중인 전화번호입니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}