package CamNecT.CamNecT_Server.domain.profile.components.majors.repository;

import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Majors, Long> {


    List<Majors> findByInstitution_InstitutionIdOrderByMajorNameKorAsc(Long institutionId);

    List<Majors> findByInstitution_InstitutionIdOrderByMajorNameEngAsc(Long institutionId);

    Optional<Majors> findByMajorIdAndInstitution_InstitutionId(
            Long majorId,
            Long institutionId
    );

    @Query("select m.majorNameKor from Majors m where m.majorId = :id")
    Optional<String> findNameKorById(@Param("id") Long id);

}
