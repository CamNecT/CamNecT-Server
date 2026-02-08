package CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains;

import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GifticonErrorCode implements BaseErrorCode {

    // 470xx - 요청/검증
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, 47001, "판매 중인 상품이 아닙니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, 47002, "수량이 올바르지 않습니다."),
    INVALID_SPEND_POINTS(HttpStatus.BAD_REQUEST, 47003, "차감 포인트 값이 올바르지 않습니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, 47004, "포인트가 부족합니다."),

    // 474xx - 리소스 없음
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, 47401, "상품을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,47402, "유저를 찾을 수 없습니다."),

    // 479xx - 상태/규칙 위반
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, 49401, "이미 처리된 요청입니다."),

    // 57xxx - 서버에러
    EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 57001, "엑셀 내보내기에 실패했습니다."),
    VENDOR_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 57002, "업체 상품 동기화에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}