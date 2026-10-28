package CamNecT.server.domain.chat.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRoomBulkExitRequest(
        @Schema(description = "퇴장할 종료된 채팅방 ID 목록 (1~100개, 중복 허용)", example = "[10, 20]")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Positive Long> roomIds
) {}
