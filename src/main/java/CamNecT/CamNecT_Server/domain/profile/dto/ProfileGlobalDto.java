package CamNecT.CamNecT_Server.domain.profile.dto;

public record ProfileGlobalDto(
        Long userId,
        String userName,
        String majorName,
        String studentNo,
        String profileImageKey // nullable
) {}