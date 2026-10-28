package CamNecT.server.domain.activity.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ActivityImportRequest(
        @NotBlank @Pattern(regexp = "[0-9]{1,20}") String articleId,
        @Min(1) long revision,
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String contentHash,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        @Size(max = 100) String organizer,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        OffsetDateTime applyEndAt,
        @Valid Thumbnail thumbnail) {
    public record Thumbnail(
            @NotBlank @Pattern(regexp = "image/(jpeg|png|webp)") String contentType,
            @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String sha256,
            @NotBlank @Size(max = 10666668) String base64) {}
}
