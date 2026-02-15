package CamNecT.server.domain.community.dto.response;

public record ToggleBookmarkResponse(
        Long postId,
        boolean bookmarked,
        long bookmarkCount
) {}
