package CamNecT.server.domain.community.dto.response;

import CamNecT.server.domain.community.model.enums.ContentAccessStatus;

public record PurchasePostAccessResponse(
        Long postId,
        ContentAccessStatus accessStatus,
        int remainingPoints,
        boolean isAlreadyOwned
) {}
