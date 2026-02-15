package CamNecT.server.global.tag.dto;

public record TagGraphBatchResult(
        int profileStats,
        int postCommunityStats,
        int postActivityStats,
        int profileRelation,
        int postCommunityRelation,
        int postActivityRelation
) {}