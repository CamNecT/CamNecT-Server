package CamNecT.CamNecT_Server.global.tag.service;

import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionListResponse;
import CamNecT.CamNecT_Server.global.tag.dto.response.InstitutionResponse;
import CamNecT.CamNecT_Server.global.tag.repository.InstitutionRepository;
import CamNecT.CamNecT_Server.global.tag.model.Institutions;
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
