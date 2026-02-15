package CamNecT.server.domain.community.dto;

public record AuthorDto(
        Long userId,
        String name,
        String profileImageUrl, // 최종 CDN URL (없으면 null)
        String studentNo,
        String majorName       // 없으면 "전공 미입력" 같은 기본값
) {}