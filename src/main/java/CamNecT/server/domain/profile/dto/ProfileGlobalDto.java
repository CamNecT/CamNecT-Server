package CamNecT.server.domain.profile.dto;

public record ProfileGlobalDto(
        Long userId,
        String userName,
        String majorName,
        String studentNo,
        String profileImageKey // nullable
) {}