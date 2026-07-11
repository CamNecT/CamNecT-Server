package CamNecT.server.domain.profile.dto.request;

import java.util.List;

public record UpdateProfileTagsRequest(
        List<Long> tagIds
) {}
