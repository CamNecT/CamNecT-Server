package CamNecT.server.domain.chat.dto.request.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequestSendDto(
        @NotNull @Positive Long receiverId,
        List<@NotNull @Positive Long> tagIds,
        @NotBlank @Size(max = 16000) String content
) {
}
