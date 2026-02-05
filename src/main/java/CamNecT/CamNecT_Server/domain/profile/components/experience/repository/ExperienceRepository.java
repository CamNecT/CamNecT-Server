package CamNecT.CamNecT_Server.domain.profile.components.experience.repository;

import CamNecT.CamNecT_Server.domain.profile.components.experience.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findAllByUser_UserIdOrderByStartDateDesc(Long userId);
}