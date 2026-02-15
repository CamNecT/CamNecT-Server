package CamNecT.server.domain.profile.dto.response;

import CamNecT.server.domain.users.model.UserStatus;

public record ProfileStatusResponse(
        UserStatus status
) {}
