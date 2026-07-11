package CamNecT.server.domain.community.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(max = 200) String title,
        @Size(max = 20000) String content,
        Boolean anonymous,
        List<Long> tagIds,
        List<@Valid AttachmentRequest> attachments
) {}
