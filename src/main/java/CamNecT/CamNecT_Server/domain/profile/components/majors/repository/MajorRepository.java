package CamNecT.CamNecT_Server.domain.profile.components.majors.repository;

import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Majors, Long> {


    List<Majors> findByInstitution_InstitutionIdOrderByMajorNameKorAsc(Long institutionId);

    List<Majors> findByInstitution_InstitutionIdOrderByMajorNameEngAsc(Long institutionId);

    Optional<Majors> findByMajorIdAndInstitution_InstitutionId(
            Long majorId,
            Long institutionId
    );

}
