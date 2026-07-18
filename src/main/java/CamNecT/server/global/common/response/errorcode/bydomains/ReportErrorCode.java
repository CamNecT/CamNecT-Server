package CamNecT.server.global.common.response.errorcode.bydomains;

import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    // 51xxx - 신고 도메인 (xx000은 전역 HTTP 오류 예약)
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, 51401, "해당 신고가 존재하지 않습니다."),
    REPORT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, 51001, "이미 처리된 신고입니다."),
    REPORT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, 51002, "자기 자신을 신고할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
