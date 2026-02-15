package CamNecT.server.domain.community.dto.response;

public record ToggleCommentLikeResponse(
        boolean liked,
        long likeCount
) {}
