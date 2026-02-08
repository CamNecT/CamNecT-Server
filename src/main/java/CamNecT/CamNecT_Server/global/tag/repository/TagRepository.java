package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

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

