package CamNecT.CamNecT_Server.domain.community.dto.response;

import CamNecT.CamNecT_Server.domain.community.model.enums.ContentAccessStatus;

public record PurchasePostAccessResponse(
        Long postId,
        ContentAccessStatus accessStatus,
        int remainingPoints,
        boolean isAlreadyOwned
) {}
