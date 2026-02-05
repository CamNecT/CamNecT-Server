package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {
    List<TagCategory> findAllByActiveTrueOrderBySortOrderAscIdAsc();
    Optional<TagCategory> findByCodeAndActiveTrue(String code);
}