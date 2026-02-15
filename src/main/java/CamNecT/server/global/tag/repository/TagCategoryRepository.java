package CamNecT.server.global.tag.repository;

import CamNecT.server.global.tag.model.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {

    List<TagCategory> findAllByActiveTrueAndCodeInOrderBySortOrderAscIdAsc(List<String> codes);

    @Query("""
        SELECT c.code
        FROM TagCategory c
        WHERE c.active = true
          AND c.code NOT IN :excludedCodes
        ORDER BY c.sortOrder ASC, c.id ASC
    """)
    List<String> findBaseCategoryCodesExcluding(@Param("excludedCodes") List<String> excludedCodes);
}