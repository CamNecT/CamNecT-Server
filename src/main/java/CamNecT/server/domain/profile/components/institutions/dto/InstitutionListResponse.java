package CamNecT.server.domain.profile.components.institutions.dto;

import CamNecT.server.domain.profile.components.institutions.model.Institutions;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class InstitutionListResponse {
    private List<InstitutionResponse> institutions;

    public static InstitutionListResponse from(List<Institutions> institutions) {
        return InstitutionListResponse.builder()
                .institutions(institutions.stream()
                        .map(InstitutionResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    public static InstitutionListResponse empty() {
        return InstitutionListResponse.builder()
                .institutions(Collections.emptyList())
                .build();
    }
}