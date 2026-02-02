package CamNecT.CamNecT_Server.global.common.util;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException e) {
        BaseErrorCode ec = e.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus())
                .body(new ErrorResponse(ec.getHttpStatus().value(), ec.getCode(), ec.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValid(MethodArgumentNotValidException e) {
        // 원하면 필드 메시지를 만들 수도 있고, 일단은 BAD_REQUEST로 통일해도 됨
        ErrorCode ec = ErrorCode.BAD_REQUEST;
        return ResponseEntity.status(ec.getHttpStatus())
                .body(new ErrorResponse(ec.getHttpStatus().value(), ec.getCode(), ec.getMessage()));
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleEtc(Exception e) {
//        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
//        return ResponseEntity.status(ec.getHttpStatus())
//                .body(new ErrorResponse(ec.getHttpStatus().value(), ec.getCode(), ec.getMessage()));
//    }
}
