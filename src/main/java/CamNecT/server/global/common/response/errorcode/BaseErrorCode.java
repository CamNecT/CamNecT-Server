package CamNecT.server.global.common.response.errorcode;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    int getCode();
    String getMessage();
}
