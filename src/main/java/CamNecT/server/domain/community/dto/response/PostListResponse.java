package CamNecT.server.domain.community.dto.response;

import java.util.List;

public record PostListResponse(
        List<PostSummaryResponse> items,
        Long nextCursorId,
        Long nextCursorValue,   // ✅ 추가 (hotScore / likeCount / bookmarkCount)
        boolean hasNext
) {
    public static PostListResponse of(List<PostSummaryResponse> items, boolean hasNext, Long nextCursorValue) {
        Long nextCursorId = items.isEmpty() ? null : items.getLast().postId();
        return new PostListResponse(items, nextCursorId, nextCursorValue, hasNext);
    }
}
