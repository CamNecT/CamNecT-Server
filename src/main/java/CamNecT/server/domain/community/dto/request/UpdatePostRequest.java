package CamNecT.server.domain.community.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(max = 200) String title,
        String content,
        Boolean anonymous,
        List<Long> tagIds,
        List<AttachmentRequest> attachments
) {}
