package CamNecT.CamNecT_Server.global.common.util;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.CamNecT_Server.global.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
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
}