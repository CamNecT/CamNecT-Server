package CamNecT.server.domain.community.dto.request;

import CamNecT.server.global.storage.dto.request.PresignUploadBatchRequest.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CommunityAttachmentPresignRequest(
        List<@NotNull @Valid Item> items,
        @Schema(description = "실패/취소하여 교체할 미사용 fileKey 목록. 지정한 본인 커뮤니티 티켓만 만료시킵니다. 미전송 시 기존 추가 발급 동작을 유지합니다.")
        @Size(max = 3) List<@NotBlank @Size(max = 500) String> replaceFileKeys
) {
    public CommunityAttachmentPresignRequest(List<Item> items) {
        this(items, List.of());
    }
}
