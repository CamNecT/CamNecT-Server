package CamNecT.CamNecT_Server.domain.profile.components.majors.service;

import CamNecT.CamNecT_Server.domain.profile.components.majors.dto.MajorListResponse;
import CamNecT.CamNecT_Server.domain.profile.components.majors.dto.MajorResponse;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorService {

    private final InstitutionRepository institutionRepository;
    private final MajorRepository majorRepository;

    public MajorListResponse getMajors(Long institutionId) {
        if (!institutionRepository.existsById(institutionId)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        List<MajorResponse> items = majorRepository
                .findByInstitution_InstitutionIdOrderByMajorNameKorAsc(institutionId)
                .stream()
                .map(MajorResponse::from)
                .toList();

        return MajorListResponse.builder()
                .items(items)
                .build();
    }

    public MajorResponse getMajor(Long institutionId, Long majorId) {
        Majors majors = majorRepository.findByMajorIdAndInstitution_InstitutionId(majorId, institutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        return MajorResponse.from(majors);
    }
}