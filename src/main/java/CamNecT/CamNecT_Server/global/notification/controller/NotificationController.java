package CamNecT.CamNecT_Server.global.notification.controller;

import CamNecT.CamNecT_Server.global.common.auth.UserId;
import CamNecT.CamNecT_Server.global.common.response.ApiResponse;
import CamNecT.CamNecT_Server.global.notification.dto.response.NotificationItemResponse;
import CamNecT.CamNecT_Server.global.notification.dto.response.NotificationListResponse;
import CamNecT.CamNecT_Server.global.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListResponse> list(
            @UserId Long userId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        var slice = notificationService.listItems(userId, cursorId, size);

        var items = slice.getContent();
        Long nextCursor = items.isEmpty() ? null : items.getLast().id();

        return ApiResponse.success(new NotificationListResponse(items, nextCursor, slice.hasNext()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@UserId Long userId) {
        return ApiResponse.success(new UnreadCountResponse(notificationService.countUnread(userId)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@UserId Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<MarkAllReadResponse> markAllRead(@UserId Long userId) {
        int updated = notificationService.markAllRead(userId);
        long unread = notificationService.countUnread(userId);
        return ApiResponse.success(new MarkAllReadResponse(updated, unread));
    }




    public record UnreadCountResponse(long unreadCount) {}
    public record MarkAllReadResponse(int updatedCount, long unreadCount) {}
}
