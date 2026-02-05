package CamNecT.CamNecT_Server.domain.profile.components.majors.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MajorListResponse {
    private List<MajorResponse> items;
}
