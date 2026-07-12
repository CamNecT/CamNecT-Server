package CamNecT.server.domain.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecruitmentApplyRequest(
        @NotBlank @Size(max = 100) String content
) {
}
