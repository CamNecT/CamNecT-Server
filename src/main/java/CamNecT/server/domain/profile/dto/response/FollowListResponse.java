package CamNecT.server.domain.profile.dto.response;

import java.util.List;

public record FollowListResponse(
        List<FollowUserDetailDto> users,
        long count
) {
    public record FollowUserDetailDto(
            Long userId,
            String name,
            String majorName,
            String studentNo,
            String profileImageUrl
    ) {
    }
}