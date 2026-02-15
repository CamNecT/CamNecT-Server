package CamNecT.server.domain.profile.components.institutions.repository;

import CamNecT.server.domain.profile.components.institutions.model.Institutions;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface InstitutionRepository extends JpaRepository<Institutions, Long> {
//    List<Institutions> findAllByOrderByInstitutionNameKorAsc();

    @Query("SELECT i FROM Institutions i " +
            "WHERE (i.institutionNameKor LIKE %:keyword% OR i.institutionNameEng LIKE %:keyword%) " +
            "AND i.isActive = true " +
            "ORDER BY i.institutionNameKor ASC")
        // 가나다순 정렬
    List<Institutions> searchActiveInstitutions(@Param("keyword") String keyword, Pageable pageable);

    @Query("select i.institutionNameKor from Institutions i where i.institutionId = :id")
    Optional<String> findNameKorById(@Param("id") Long id);

}
