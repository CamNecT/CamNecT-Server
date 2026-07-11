package CamNecT.server.global.common.util;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.exception.InvalidPropertiesException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.common.response.InvalidPropertiesErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException e, HttpServletRequest req) {
        BaseErrorCode errorCode = e.getErrorCode();

        log.warn("[CustomException] {} {} | status={} code={}",
                req.getMethod(),
                req.getRequestURI(),
                errorCode.getHttpStatus().value(),
                errorCode.getCode());

        return response(errorCode);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest req) {
        log.warn("[BadRequest] {} {} | code={} | exception={} | ua={}",
                req.getMethod(),
                req.getRequestURI(),
                ErrorCode.BAD_REQUEST.getCode(),
                e.getClass().getSimpleName(),
                req.getHeader("User-Agent")
        );

        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(new ErrorResponse(
                        ErrorCode.BAD_REQUEST.getHttpStatus().value(),
                        ErrorCode.BAD_REQUEST.getCode(),
                        ErrorCode.BAD_REQUEST.getMessage()
                ));
    }

    @ExceptionHandler(InvalidPropertiesException.class)
    public ResponseEntity<InvalidPropertiesErrorResponse> handleInvalidProperties(
            InvalidPropertiesException e,
            HttpServletRequest req
    ) {
        log.warn("[InvalidPropertiesException] {} {} | status={} code={} | invalidProperties={} | ua={}",
                req.getMethod(),
                req.getRequestURI(),
                e.getHttpStatus().value(),
                e.getCode(),
                e.getInvalidProperties(),
                req.getHeader("User-Agent")
        );

        return ResponseEntity.status(e.getHttpStatus())
                .body(new InvalidPropertiesErrorResponse(
                        e.getHttpStatus().value(),
                        e.getCode(),
                        e.getMessage(),
                        e.getInvalidProperties()
                ));
    }


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(Exception e, HttpServletRequest req) {
        log.warn("[UnreadableBody] {} {}", req.getMethod(), req.getRequestURI());
        return response(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(Exception e) {
        return response(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(Exception e) {
        return response(ErrorCode.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(Exception e) {
        return response(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(Exception e) {
        return response(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
        return response(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest req) {
        log.error("[UnexpectedException] {} {}", req.getMethod(), req.getRequestURI(), e);
        return response(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> response(BaseErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(new ErrorResponse(
                        errorCode.getHttpStatus().value(),
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }
}