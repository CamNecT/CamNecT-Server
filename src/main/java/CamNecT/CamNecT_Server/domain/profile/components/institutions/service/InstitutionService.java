package CamNecT.CamNecT_Server.domain.profile.components.institutions.service;

import CamNecT.CamNecT_Server.domain.profile.components.institutions.dto.InstitutionListResponse;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.dto.InstitutionResponse;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.model.Institutions;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionListResponse searchInstitutions(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        List<Institutions> institutions = institutionRepository.searchActiveInstitutions(keyword, limit);

        return InstitutionListResponse.from(institutions);
    }

    public InstitutionResponse getInstitution(Long id) {
        Institutions institution = institutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("기관 없음"));

        return InstitutionResponse.from(institution);
    }
}
