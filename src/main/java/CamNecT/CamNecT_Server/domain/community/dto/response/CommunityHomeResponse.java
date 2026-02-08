package CamNecT.CamNecT_Server.domain.community.dto.response;

import java.util.List;

public record CommunityHomeResponse(
        Long tagId,
        String tagName,
        List<PostSummaryResponse> recommendedByTag,
        List<PostSummaryResponse> waitingQuestions
) {}
