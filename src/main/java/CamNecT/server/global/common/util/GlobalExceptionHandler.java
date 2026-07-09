package CamNecT.server.global.common.util;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.exception.InvalidPropertiesException;
import CamNecT.server.global.common.response.errorcode.BaseErrorCode;
import CamNecT.server.global.common.response.ErrorResponse;
import CamNecT.server.global.common.response.InvalidPropertiesErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException e, HttpServletRequest req) {

        BaseErrorCode ec = e.getErrorCode();

        // 토큰 원문은 절대 찍지 마세요. (있/없만)
        String auth = req.getHeader("Authorization");
        boolean hasAuth = (auth != null && !auth.isBlank());

        Object userIdAttr = req.getAttribute("userId"); // AuthInterceptor에서 setAttribute 한 값

        log.warn("[CustomException] {} {} | status={} code={} | userIdAttr={} | hasAuth={} | ua={}",
                req.getMethod(),
                req.getRequestURI(),
                ec.getHttpStatus().value(),
                ec.getCode(),
                userIdAttr,
                hasAuth,
                req.getHeader("User-Agent")
        );

        // 원인 파악 필요하면(개발/스테이징만) cause도 같이:
        // log.warn("cause=", e);

        return ResponseEntity.status(ec.getHttpStatus())
                .body(new ErrorResponse(ec.getHttpStatus().value(), ec.getCode(), ec.getMessage()));
    }
}
