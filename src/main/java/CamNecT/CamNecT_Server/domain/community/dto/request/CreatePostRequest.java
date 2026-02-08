package CamNecT.CamNecT_Server.domain.community.dto.request;

import CamNecT.CamNecT_Server.domain.community.model.enums.BoardCode;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostAccessType;
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
) {
    public boolean isAnonymous() { return Boolean.TRUE.equals(anonymous); }
}
