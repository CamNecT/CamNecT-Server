package CamNecT.server.domain.community.dto.response;

public record ToggleLikeResponse(
        boolean liked,
        long likeCount
) {}
