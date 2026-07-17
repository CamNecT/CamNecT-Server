package CamNecT.server.domain.chat.dto.message;

import CamNecT.server.global.common.response.errorcode.BaseErrorCode;

public record ChatSocketErrorResponse(
        String type,
        int status,
        int code,
        String message,
        String operation,
        Long roomId,
        String clientMessageId
) {
    public static ChatSocketErrorResponse of(
            BaseErrorCode errorCode,
            String operation,
            Long roomId,
            String clientMessageId
    ) {
        return new ChatSocketErrorResponse(
                "ERROR",
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                operation,
                roomId,
                clientMessageId
        );
    }
}
