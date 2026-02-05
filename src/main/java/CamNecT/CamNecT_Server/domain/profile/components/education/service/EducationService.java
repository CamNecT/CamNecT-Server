package CamNecT.CamNecT_Server.domain.profile.components.education.service;

import CamNecT.CamNecT_Server.domain.profile.components.education.dto.request.EducationRequest;
import CamNecT.CamNecT_Server.domain.profile.components.education.dto.response.EducationResponse;
import CamNecT.CamNecT_Server.domain.profile.components.education.model.Education;
import CamNecT.CamNecT_Server.domain.profile.components.education.repository.EducationRepository;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.repository.InstitutionRepository;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.domain.profile.components.institutions.model.Institutions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationService {

    private final EducationRepository educationRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final MajorRepository majorRepository;

    @Transactional
    public void addEducation(Long userId, EducationRequest request) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Institutions institution = institutionRepository.findById(request.institutionId())
                .orElseThrow(() -> new CustomException(UserErrorCode.INSTITUTION_NOT_FOUND));


        Education education = Education.builder()
                .user(user)
                .institution(institution)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status())
                .description(request.description())
                .build();

        educationRepository.save(education);
    }

    public List<EducationResponse> getMyEducations(Long userId) {
        return educationRepository.findAllByUserIdWithDetails(userId)
                .stream()
                .map(EducationResponse::from)
                .toList();
    }

    @Transactional
    public void updateEducation(Long userId, Long educationId, EducationRequest request) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new CustomException(UserErrorCode.EDUCATION_NOT_FOUND));

        // 본인 확인
        if (!education.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.EDUCATION_FORBIDDEN);
        }

        Institutions institution = institutionRepository.findById(request.institutionId())
                .orElseThrow(() -> new CustomException(UserErrorCode.INSTITUTION_NOT_FOUND));


        education.updateEducation(
                institution,
                request.startDate(),
                request.endDate(),
                request.status(),
                request.description()
        );
    }

    @Transactional
    public void deleteEducation(Long userId, Long educationId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new CustomException(UserErrorCode.EDUCATION_NOT_FOUND));

        if (!education.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.EDUCATION_FORBIDDEN);
        }
        educationRepository.delete(education);
    }
}