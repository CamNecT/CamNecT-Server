package CamNecT.CamNecT_Server.domain.profile.dto.response;

public record ProfileSettingsResponse(
        Long userId,
        String name,
        String profileImageUrl,
        String phoneNum,
        String email
) {
}
