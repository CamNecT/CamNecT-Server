package CamNecT.server.domain.profile.components.institutions.dto;

import CamNecT.server.domain.profile.components.institutions.model.Institutions;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InstitutionResponse {

    private Long id;
    private String code;
    private String nameKor;
    private String nameEng;

    public static InstitutionResponse from(Institutions institution) {
        return InstitutionResponse.builder()
                .id(institution.getInstitutionId())
                .code(institution.getInstitutionCode())
                .nameKor(institution.getInstitutionNameKor())
                .nameEng(institution.getInstitutionNameEng())
                .build();
    }
}
