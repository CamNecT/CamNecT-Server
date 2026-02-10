package CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains;

import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements BaseErrorCode {

    //460xx - 입력/검증
    SELF_APPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, 46001, "본인이 작성한 모집 글에는 신청할 수 없습니다."),
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, 46002, "이미 신청한 모집 공고입니다."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, 46003, "신청이 마감되었습니다."),

    //461xx - 권한
    NOT_AUTHOR(HttpStatus.UNAUTHORIZED, 46101, "게시글 작성자가 아닙니다."),

    //462xx - 리소스
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, 46201, "해당 대외활동을 찾을 수 없습니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 46202, "팀원 모집 글을 찾을 수 없습니다."),
    MAJOR_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, 46203, "전공 정보를 찾을 수 없습니다.");


    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
