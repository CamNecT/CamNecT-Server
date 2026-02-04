package CamNecT.CamNecT_Server.domain.experience.service;

import CamNecT.CamNecT_Server.domain.experience.dto.request.ExperienceRequest;
import CamNecT.CamNecT_Server.domain.experience.dto.response.ExperienceResponse;
import CamNecT.CamNecT_Server.domain.experience.model.Experience;
import CamNecT.CamNecT_Server.domain.experience.repository.ExperienceRepository;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addExperience(Long userId, ExperienceRequest request) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Experience experience = Experience.builder()
                .user(user)
                .companyName(request.companyName())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(request.isCurrent())
                .responsibilities(request.responsibilities())
                .build();

        experienceRepository.save(experience);
    }

    public List<ExperienceResponse> getMyExperience(Long userId) {
        return experienceRepository.findAllByUser_UserIdOrderByStartDateDesc(userId)
                .stream()
                .map(ExperienceResponse::from)
                .toList();
    }

    @Transactional
    public void updateExperience(Long userId, Long experienceId, ExperienceRequest request) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new CustomException(UserErrorCode.EXPERIENCE_NOT_FOUND));
        // 본인 확인
        if (!experience.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.EXPERIENCE_FORBIDDEN);
        }

        experience.updateExperience(
                request.companyName(),
                request.startDate(),
                request.endDate(),
                request.isCurrent(),
                request.responsibilities()
        );
    }

    @Transactional
    public void deleteExperience(Long userId, Long experienceId) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new CustomException(UserErrorCode.EXPERIENCE_NOT_FOUND));

        if (!experience.getUser().getUserId().equals(userId)) {
            throw new CustomException(UserErrorCode.EXPERIENCE_FORBIDDEN);
        }

        experienceRepository.delete(experience);
    }
}
