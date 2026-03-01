package CamNecT.server.domain.portfolio.dto.request;

import java.time.LocalDate;
import java.util.List;

public record PortfolioRequest(
        String projectTitle,
        String description,
        LocalDate startedAt,
        LocalDate endedAt,
        String project_role, // assignedRole에 매핑될 필드
        List<String> techStack, // 추가
        String review,
        String thumbnailKey,
        List<String> attachmentKeys
) {
    // null 방어: attachmentKeys가 null로 들어오면 빈 리스트로 초기화
    public PortfolioRequest {
        techStack = (techStack == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(techStack);
    }
}
