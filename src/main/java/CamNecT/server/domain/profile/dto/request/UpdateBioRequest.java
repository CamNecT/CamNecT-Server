package CamNecT.server.domain.profile.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBioRequest(
        @Size(max = 100, message = "자기소개는 100자 이하여야 합니다.")
        String bio
) {
}
