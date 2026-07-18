package CamNecT.server.domain.chat.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageSendRequestDto(
        @NotNull @Positive Long roomId,
        @NotBlank @Size(max = MAX_CONTENT_LENGTH) String content,
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "clientMessageId must be a UUID"
        )
        String clientMessageId
) {
    public static final int MAX_CONTENT_LENGTH = 16000;
}
