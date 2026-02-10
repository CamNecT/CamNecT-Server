package CamNecT.CamNecT_Server.domain.community.dto;

public record AuthorDto(
        Long userId,
        String name,
        String profileImageUrl, // 최종 CDN URL (없으면 null)
        String majorName       // 없으면 "전공 미입력" 같은 기본값
) {}