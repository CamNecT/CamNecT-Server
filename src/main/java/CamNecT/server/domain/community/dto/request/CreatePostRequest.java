package CamNecT.server.domain.community.dto.request;

import CamNecT.server.domain.community.model.enums.BoardCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotNull BoardCode boardCode,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        Boolean anonymous,
        List<Long> tagIds, // <- global tags.tag_id들
        List<AttachmentRequest> attachments
){}
