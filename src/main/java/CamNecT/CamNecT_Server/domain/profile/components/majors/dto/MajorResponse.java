package CamNecT.CamNecT_Server.domain.profile.components.majors.dto;

import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MajorResponse {
    private Long id;
    private String code;
    private String nameKor;
    private String nameEng;

    public static MajorResponse from(Majors major) {
        return MajorResponse.builder()
                .id(major.getMajorId())
                .code(major.getMajorCode())
                .nameKor(major.getMajorNameKor())
                .nameEng(major.getMajorNameEng())
                .build();
    }
}
