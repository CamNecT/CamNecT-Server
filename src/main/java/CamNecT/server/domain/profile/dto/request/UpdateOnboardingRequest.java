package CamNecT.server.domain.profile.dto.request;

import com.google.firebase.database.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOnboardingRequest(
        @Nullable
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String profileImageKey,

        @Nullable
        @Size(max = 100, message = "자기소개는 100자 이하여야 합니다.")
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String bio,

        @Nullable
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<Long> tagIds
) {}
