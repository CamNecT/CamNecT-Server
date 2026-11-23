package CamNecT.server.global.tag.repository;

import CamNecT.server.global.tag.model.Tag;
import CamNecT.server.global.tag.dto.TagDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("""
        SELECT new CamNecT.server.global.tag.dto.TagDto(t.id, t.name)
        FROM Tag t JOIN t.category c
        WHERE t.id IN :ids AND t.active = true AND c.active = true
          AND c.code NOT IN ('temp_tags', 'adoption_status', 'recruit_status')
    """)
    List<TagDto> findSelectableForPost(@Param("ids") Collection<Long> ids);

    @Query("""
        SELECT t
        FROM Tag t
        JOIN FETCH t.category c
        WHERE c.id IN :categoryIds
          AND t.active = true
        ORDER BY c.sortOrder ASC, c.id ASC, t.name ASC, t.id ASC
    """)
    List<Tag> findActiveByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    @Query("""
        SELECT t.id
        FROM Tag t
        WHERE t.id IN :ids
          AND t.active = true
    """)
    List<Long> findExistingActiveIds(@Param("ids") List<Long> ids);

    @Query("""
        SELECT t.name
        FROM Tag t
        WHERE t.id IN :ids
        ORDER BY t.name ASC
    """)
    List<String> findNamesByIds(@Param("ids") List<Long> ids);

}

