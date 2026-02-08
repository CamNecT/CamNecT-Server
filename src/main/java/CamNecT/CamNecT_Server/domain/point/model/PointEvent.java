package CamNecT.CamNecT_Server.domain.point.model;

public record PointEvent(
        //여기에는 추후 수정 반드시 필요(임시로 post쪽만 고려해서 작성)
        PointSource source,
        Long postId,
        Long requestId,
        String eventKey
) {
    public static PointEvent none(PointSource source) {
        return new PointEvent(source, null, null, null);
    }

    public static PointEvent postAccess(Long userId, Long postId) {
        String key = "POST_ACCESS:" + userId + ":" + postId;
        return new PointEvent(PointSource.POST_ACCESS_PURCHASE, postId, null, key);
    }

    public static PointEvent signup(Long userId) {
        String key = "SIGNUP:" + userId;
        return new PointEvent(PointSource.SIGNUP,null, null, key);
    }

    public static PointEvent coffeeChatRequest(Long userId, Long requestId) {
        String key = "COFFEECHAT_REQUEST:" + userId + ":" + requestId;
        return new PointEvent(PointSource.COFFEECHAT_REQUEST, null, requestId, key);
    }

    public static PointEvent commentSelection(Long receiverId, Long postId, Long commentId) {
        // 채택은 "postId+commentId" 조합으로 1번만 일어나야 하므로 이 키가 멱등키
        String key = "COMMENT_SELECTION:" + receiverId + ":" + postId + ":" + commentId;
        return new PointEvent(PointSource.COMMENT_SELECTION, postId, null, key);
    }
}
