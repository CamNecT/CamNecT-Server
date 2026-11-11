package CamNecT.server.domain.activity.dto.response;

import CamNecT.server.domain.community.dto.response.CommentItemResponse;
import java.util.List;

public record ActivityCommentListResponse(
        List<CommentItemResponse> items,
        Long nextCursorId,
        boolean hasNext,
        List<ReplyPage> replyPages
) {
    public record ReplyPage(Long parentCommentId, Long nextCursorId, boolean hasNext) {}
}
