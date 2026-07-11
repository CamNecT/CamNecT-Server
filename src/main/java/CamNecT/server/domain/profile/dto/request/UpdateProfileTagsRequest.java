package CamNecT.server.domain.profile.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProfileTagsRequest(
        List<@NotNull Long> tagIds
) {}
