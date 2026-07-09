package CamNecT.server.global.common.response;

import java.util.List;

public record InvalidPropertiesErrorResponse(
        int status,
        int code,
        String message,
        List<String> invalidProperties
) {}
