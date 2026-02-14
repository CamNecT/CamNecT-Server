package CamNecT.server.domain.profile.dto.request;

public record UpdateProfileImageRequest(
        String profileImageKey // null이면 "이미지 삭제"로 처리 가능
) {}