package CamNecT.CamNecT_Server.global.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationItemResponse> items,
        Long nextCursorId,
        boolean hasNext
) {}