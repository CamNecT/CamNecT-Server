package CamNecT.server.global.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class InvalidPropertiesException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Invalid account recovery properties.";

    private final HttpStatus httpStatus;
    private final int code;
    private final List<String> invalidProperties;

    public InvalidPropertiesException(List<String> invalidProperties) {
        super(DEFAULT_MESSAGE);
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.code = 41030;
        this.invalidProperties = List.copyOf(invalidProperties);
    }
}
